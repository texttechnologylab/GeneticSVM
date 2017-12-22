package org.geneticsvm;

import java.io.*;
import java.util.*;

public class GeneticSVMStudy extends Thread {

	private File parameterFile;
	private File indexFile;
	private File dataFile;
	private File outputFile;
	private File tmpDirectory;
	private File featureFile;
	private int maxPopulation;
	private int turns;
	private int threads;
	private double permutationRate;
	private int[] index;
	private int[] indexLabels;
	private double[][] matrix;
	private List<SVMParameters> parameters;
	private Set<Creature> population;
	private int elapsedTurns;
	private long timeout;
	private File resumeFile;
	private List<Creature> presetPopulation = null;
	private String features = null;
	private PrintWriter writer;
	private double lastTurnsBestF = 0; // For Debugging
	
	private void checkFiles() {
		if (parameterFile == null) {
			System.err.println("Parameter File not specified!");
			System.exit(-1);
		}
		if (!parameterFile.canRead()) {
			System.err.println("Parameter File not accessible");
			System.exit(-1);
		}
		if (indexFile == null) {
			System.err.println("Index File not specified!");
			System.exit(-1);
		}
		if (!indexFile.canRead()) {
			System.err.println("Index File not accessible");
			System.exit(-1);
		}
		if (dataFile == null) {
			System.err.println("Data File not specified!");
			System.exit(-1);
		}
		if (!dataFile.canRead()) {
			System.err.println("Data File not accessible");
			System.exit(-1);
		}
		if (featureFile != null) {
			if (!featureFile.canRead()) {
				System.err.println("Feature File specified but not accessible");
				System.exit(-1);
			}
		}
	}
	
	public GeneticSVMStudy(File pParameterFile, File pIndexFile, File pDataFile, File pFeatureFile, File pResumeFile, File pOutputFile, File pTmpDirectory, int pMaxPopulation, int pTurns, int pThreads, double pPermutationRate, long pTimeout) {
		parameterFile = pParameterFile;
		indexFile = pIndexFile;
		dataFile = pDataFile;
		outputFile = pOutputFile;
		tmpDirectory = pTmpDirectory;
		maxPopulation = pMaxPopulation;
		turns = pTurns;
		threads = pThreads;
		permutationRate = pPermutationRate;
		timeout = pTimeout * 1000;
		featureFile = pFeatureFile;
		resumeFile = pResumeFile;
	}
	
	private void checkSanity() {
		if (maxPopulation<3) {
			System.err.println("Population < 3 not recommended");
			System.exit(-1);
		}
		if (turns < 1) {
			System.err.println("Number of turns < 1 is invalid");
			System.exit(-1);
		}
		if (threads < 1) {
			System.err.println("Number of threads < 1 is invalid");
			System.exit(-1);
		}
		if (threads > Runtime.getRuntime().availableProcessors()) {
			System.err.println("Number of threads > number of CPU Cores ("+Runtime.getRuntime().availableProcessors()+") not recommended");
			System.exit(-1);
		}
		if ((permutationRate<0) || (permutationRate>1.0)) {
			System.err.println("Permutation rate must be in range of [0,1]");
			System.exit(-1);
		}
		int lFeatures = matrix[0].length;
		int lRows = matrix.length;
		if (index.length != lRows) {
			System.err.println("Number of rows of data matrix ("+lRows+") and index ("+index.length+") differ!");
			System.exit(-1);
		}
		if (parameters == null) {
			System.err.println("No parameters found");
			System.exit(-1);
		}
		if (parameters.size()==0) {
			System.err.println("No parameters found");
			System.exit(-1);
		}
	}
	
	public void run() {
		try {
			// Init
			if (resumeFile != null) parseResumeFile();
			checkFiles();
			if (featureFile != null) parseFeatureFile();
			parseIndex();
			parseMatrix();
			parseParameters();
			checkSanity();
			prepareDirectories();
			createInitialPopulation();
			// Compute
			writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
			writer.println("Genetic SVM Study Report");
			writer.println("parameterFile: "+parameterFile.getAbsolutePath());
			writer.println("indexFile: "+indexFile.getAbsolutePath());
			writer.println("dataFile: "+dataFile.getAbsolutePath());
			if (featureFile != null) writer.println("featureFile: "+featureFile.getAbsolutePath());
			for (elapsedTurns=0; elapsedTurns<turns; elapsedTurns++) {
				System.out.println("Computing Turn: "+(elapsedTurns+1)+"/"+turns);
				// Evaluate Population
				ComputeCreatureThread[] lPool = new ComputeCreatureThread[threads];
				int lFinished = 0;
				for (Creature lCreature:population) {
					int lID = -1;
					while (lID == -1) {
						for (int i=0; i<lPool.length; i++) {
							if (lPool[i] == null) {
								lID = i;
							}
							else if (lPool[i].getState() == Thread.State.TERMINATED) {
								if (!lPool[i].isTimeout()) {
									lFinished++;
									System.out.println("  "+lFinished+"/"+population.size()+" "+lPool[i].getCreatue().getFMeasure()+" "+(lPool[i].getCreatue().getFeaturesCount()*100.0)/matrix[0].length+"%");
									lPool[i] = null;
									lID = i;
								}
								else {
									lFinished++;
									System.out.println("  "+lFinished+"/"+population.size()+" "+lPool[i].getCreatue().getFMeasure()+" "+(lPool[i].getCreatue().getFeaturesCount()*100.0)/matrix[0].length+"% [Aborted because of timeout. "+(lPool[i].getCreatue().isKeep()?"Keeping":"not Keeping")+"]");
									lPool[i] = null;
									lID = i;
								}
							}
							else {
								if (lPool[i].isTimeout()) {
									lPool[i].kill();
								}
							}
						}
						if (lID == -1) {
							Thread.sleep(500);
						}
					}
					lPool[lID] = new ComputeCreatureThread(lCreature, parameters, index, indexLabels, matrix, tmpDirectory, lID, timeout);
					lPool[lID].start();
				}
				boolean lAllDone = false;
				while (!lAllDone) {
					lAllDone = true;
					for (int i=0; i<lPool.length; i++) {
						if (lPool[i] != null) {
							if (lPool[i].getState() == Thread.State.TERMINATED) {
								if (!lPool[i].isTimeout()) {
									lFinished++;
									System.out.println("  "+lFinished+"/"+population.size()+" "+lPool[i].getCreatue().getFMeasure()+" "+(lPool[i].getCreatue().getFeaturesCount()*100.0)/matrix[0].length+"%");
									lPool[i] = null;
								}
								else {
									lFinished++;
									System.out.println("  "+lFinished+"/"+population.size()+" "+lPool[i].getCreatue().getFMeasure()+" "+(lPool[i].getCreatue().getFeaturesCount()*100.0)/matrix[0].length+"% [Aborted because of timeout. "+(lPool[i].getCreatue().isKeep()?"Keeping":"not Keeping")+"]");
									lPool[i] = null;
								}
							}
							else {	
								if (lPool[i].isTimeout()) {
									lPool[i].kill();
								}
								lAllDone = false;
							}
						}
					}
				}
				evaluateAndMutate();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (writer != null) writer.close();
		}
	}
	
	private void evaluateAndMutate() throws IOException {
		// Write Results of this turn
		List<Creature> lCreatures = new Vector<Creature>(population);
		Collections.sort(lCreatures);
		
		writer.println("\nTurn: "+(elapsedTurns+1));
		double lNewBestF = 0;
		double lMed = 0;
		int lMedCount = 0;
		for (int i=0; i<lCreatures.size(); i++) {
			if (lCreatures.get(i).getIndexLabelsBestResults() != null) {
				if (lCreatures.get(i).getFMeasure()>lNewBestF) {
					lNewBestF = lCreatures.get(i).getFMeasure();
				}
				lMed += lCreatures.get(i).getFMeasure();
				lMedCount++;
				StringBuffer lBuffer = new StringBuffer(lCreatures.get(i).getFMeasure()+"\t"+lCreatures.get(i).toString()+"\t"+(lCreatures.get(i).getFeaturesCount()*100.0)/matrix[0].length+"%");					
				for (int k=0; k<lCreatures.get(i).getIndexLabelsBestResults().length; k++) {
					lBuffer.append("\n");
					SVMResult lResult = lCreatures.get(i).getIndexLabelsBestResults()[k];
					lBuffer.append("\t"+"[ Label = "+lResult.getIndexLabel()+" fMeasure = "+lResult.getFMeasure()+" Precision = "+lResult.getPrecision()+" Recall = "+lResult.getRecall()+" Parameters = "+(lResult.getParameters() != null ? lResult.getParameters().toString():"null")+" ]");
				}
				writer.println(lBuffer.toString());
				if (i == 0) {
					System.out.println("  "+lBuffer.toString());
				}
			}
		}
		writer.println("Average F-Measure of turn: "+(lMed/lMedCount));
		System.out.println("  Average F-Measure of turn: "+(lMed/lMedCount));
		writer.flush();
		
		// Sanity Check
		if (lNewBestF <lastTurnsBestF) {
			System.err.println("Error: Best F-Measure of current turn worse than of previous turn");
			writer.flush();
			writer.close();
			System.exit(-1);
		}
		else {
			lastTurnsBestF = lNewBestF;
		}
		
		// Mutate				
		population.clear();
		// Keep the best 3 as they are
		for (int i=0; i<Math.min(lCreatures.size(), 3); i++) {
			population.add(lCreatures.get(i));
		}
		for (int i=0; i<lCreatures.size()-3; i++) {
			if (lCreatures.get(i).isKeep()) {
				population.add(lCreatures.get(i));
			}
			else {
				Creature lOffspring = lCreatures.get(i).createOffspring(permutationRate);
				while (population.contains(lOffspring)) {
					lOffspring = lCreatures.get(i).createOffspring(permutationRate);
				}
				population.add(lOffspring);
			}
		}
		int lSpace = maxPopulation - population.size();
		for (int i=0; i<lSpace; i++) {
			Creature lOffspring = new Creature(Math.random()+0.001, matrix[0].length);
			while (population.contains(lOffspring)) {
				lOffspring = lCreatures.get(i).createOffspring(permutationRate);
			}
			population.add(lOffspring);
		}
	}
	
	private void parseIndex() throws IOException {
		BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile),"UTF-8"));
		String lLine = null;
		List<Integer> lEntries = new Vector<Integer>();
		while ((lLine = lReader.readLine()) != null) {
			lLine = lLine.trim();
			if (lLine.length() == 0) continue;
			if (lLine.startsWith("#")) continue;
			lEntries.add(Integer.parseInt(lLine));
		}
		lReader.close();
		index = new int[lEntries.size()];
		Set<Integer> lSet = new HashSet<Integer>();
		for (int i=0; i<lEntries.size(); i++) {
			index[i] = lEntries.get(i);
			lSet.add(index[i]);
		}
		indexLabels = new int[lSet.size()];
		int k=0;
		for (Integer i:lSet) {
			indexLabels[k++] = i;
		}
		Arrays.sort(indexLabels);
	}
	
	private void parseMatrix() throws IOException {
		BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile),"UTF-8"));
		String lLine = null;
		List<List<Double>> lEntries = new Vector<List<Double>>();
		while ((lLine = lReader.readLine()) != null) {
			lLine = lLine.trim();
			if (lLine.length() == 0) continue;
			if (lLine.startsWith("#")) continue;
			lLine = lLine.replace('\t', ' ');
			lLine = lLine.replaceAll("[ ]{2,}", " ").trim();
			String[] lFields = lLine.split(" ");
			List<Double> lCurrentLine = new Vector<Double>();
			for (int i=0; i<lFields.length; i++) {
				if (features == null) {
					lCurrentLine.add(Double.parseDouble(lFields[i]));
				}
				else {
					if (features.charAt(i) == '1') {
						lCurrentLine.add(Double.parseDouble(lFields[i]));
					}
				}
			}
			if (lEntries.size()>0) {
				if (lEntries.get(0).size() != lCurrentLine.size()) {
					throw new IOException("Data Matrix contains rows of differing size");
				}
			}
			lEntries.add(lCurrentLine);
		}
		lReader.close();
		matrix = new double[lEntries.size()][lEntries.get(0).size()];
		for (int i=0; i<lEntries.size(); i++) {
			for (int k=0; k<lEntries.get(0).size(); k++) {
				matrix[i][k] = lEntries.get(i).get(k);
			}
		}
	}
	
	private void parseParameters() throws IOException {
		BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(parameterFile),"UTF-8"));
		String lLine = null;
		parameters = new Vector<SVMParameters>();
		while ((lLine = lReader.readLine()) != null) {
			lLine = lLine.trim();
			if (lLine.length() == 0) continue;
			if (lLine.startsWith("#")) continue;
			int lT = 0;
			double lC = 0;
			int lD = 0;
			double lG = 0;
			String[] lFields = lLine.split(" ");
			for (int i=0; i<lFields.length; i++) {
				if (lFields[i].equals("-t")) {
					lT = Integer.parseInt(lFields[++i]);
				}
				else if (lFields[i].equals("-g")) {
					lG = Double.parseDouble(lFields[++i]);
				}
				else if (lFields[i].equals("-c")) {
					lC = Double.parseDouble(lFields[++i]);
				}
				else if (lFields[i].equals("-d")) {
					lD = Integer.parseInt(lFields[++i]);
				}
			}
			parameters.add(new SVMParameters(lT, lC, lD, lG));
		}
		lReader.close();
	}
	
	private void prepareDirectories() throws IOException {
		tmpDirectory.mkdirs();
		for (int i=0; i<threads; i++) {
			File lFile = new File(tmpDirectory.getAbsolutePath()+File.separator+Integer.toString(i));
			lFile.mkdirs();
		}
	}
	
	private void createInitialPopulation() throws IOException {
		if (presetPopulation == null) {
			population = new HashSet<Creature>();
			// Ensure that the entire feature-set is included!
			population.add(new Creature(1, matrix[0].length));
			for (int i=0; i<maxPopulation-1; i++) {
				population.add(new Creature(Math.random()+0.001, matrix[0].length));
			}
		}
		else {
			population = new HashSet<Creature>();
			for (Creature lCreature:presetPopulation) {
				population.add(lCreature);
			}
		}
	}
	
	private void parseFeatureFile() throws IOException {
		BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(featureFile),"UTF-8"));
		String lLine = null;
		while ((lLine = lReader.readLine()) != null) {
			lLine = lLine.trim();
			if (lLine.length() == 0) continue;
			if (lLine.startsWith("#")) continue;
			break;
		}
		StringBuffer lResult = new StringBuffer();
		for (int i=0; i<lLine.length(); i++) {
			if ((lLine.charAt(i) == '0') || (lLine.charAt(i) == '1')) {
				lResult.append(lLine.charAt(i));
			}
		}		
		lReader.close();
		features = lResult.toString();
	}
	
	private void parseResumeFile() throws IOException {
		if (!resumeFile.canRead()) {
			System.err.println("Resume file specified but not accessible");
			System.exit(-1);
		}
		BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(resumeFile), "UTF-8"));
		String lLine = null;
		StringBuffer lBuffer = new StringBuffer();
		while ((lLine = lReader.readLine()) != null) {
			lBuffer.append(lLine+"\n");
			if (lLine.startsWith("parameterFile: ")) {
				parameterFile = new File(lLine.substring(lLine.indexOf(" ")+1));
			}
			else if (lLine.startsWith("indexFile: ")) {
				indexFile = new File(lLine.substring(lLine.indexOf(" ")+1));
			}
			else if (lLine.startsWith("dataFile: ")) {
				dataFile = new File(lLine.substring(lLine.indexOf(" ")+1));
			}
			else if (lLine.startsWith("featureFile: ")) {
				featureFile = new File(lLine.substring(lLine.indexOf(" ")+1));
			}
		}
		lReader.close();
		String lString = lBuffer.toString();
		if (lString.indexOf("Turn: ")>-1) {
			presetPopulation = new Vector<Creature>();
			lString = lString.substring(lString.lastIndexOf("Turn: "));
			String[] lFields = lString.split("\n");
			for (int i=0; i<lFields.length; i++) {
				if (lFields[i].length()>0) {
					if (Character.isDigit(lFields[i].charAt(0))) {
						lBuffer = new StringBuffer();
						String lFoo = lFields[i].split("\t")[1];
						for (int k=0; k<lFoo.length(); k++) {
							if ((lFoo.charAt(k)=='0') || (lFoo.charAt(k)=='1')) {
								lBuffer.append(lFoo.charAt(k));
							}
						}
						Creature lCreature = new Creature(lBuffer.toString());
						presetPopulation.add(lCreature);
						int k = 0;
						for (k=i+1; k<lFields.length; k++) {
							if (!lFields[k].trim().startsWith("[")) {
								break;
							}
						}
						SVMResult[] lResults = new SVMResult[k-i-1];
						for (int m=i+1; m<k; m++) {
							int lLabel = Integer.parseInt(lFields[m].substring(lFields[m].indexOf("Label = ")+8, lFields[m].indexOf(" fMeasure")));
							double lPrecision = Double.parseDouble(lFields[m].substring(lFields[m].indexOf("Precision = ")+12, lFields[m].indexOf(" Recall")));
							double lRecall = Double.parseDouble(lFields[m].substring(lFields[m].indexOf("Recall = ")+9, lFields[m].indexOf(" Parameters")));
							SVMParameters lParameters = SVMParameters.fromString(lFields[m].substring(lFields[m].indexOf("Parameters = ")+13, lFields[m].lastIndexOf("]")-1));
							lResults[m-(i+1)] = new SVMResult(lLabel, lParameters, lPrecision, lRecall);
						}
					}
				}
			}
		}
		if (presetPopulation.size() == 0) {
			presetPopulation = null;
		}
	}
}
