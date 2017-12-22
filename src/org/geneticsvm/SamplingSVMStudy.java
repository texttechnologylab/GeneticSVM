package org.geneticsvm;

import java.io.*;
import java.util.*;

public class SamplingSVMStudy extends Thread {

	private File parameterFile;
	private File indexFile;
	private File dataFile;
	private File featureFile;
	private File outputFile;
	private File tmpDirectory;
	private int threads;
	private int[] index;
	private int[] indexLabels;
	private SVMParameters[] indexLabelParameters;
	private double[][] matrix;
	private int elapsedCycles;
	private long timeout;
	private String samplingMode;
	private int minSamples;
	private int samplingCycles;
	private String features;
	
	public SamplingSVMStudy(File pParameterFile, File pIndexFile, File pDataFile, File pFeatureFile, File pOutputFile, File pTmpDirectory, String pSamplingMode, int pMinSamples, int pSamplingCycles, int pThreads, long pTimeout) {
		parameterFile = pParameterFile;
		indexFile = pIndexFile;
		dataFile = pDataFile;
		outputFile = pOutputFile;
		tmpDirectory = pTmpDirectory;
		threads = pThreads;
		timeout = pTimeout * 1000;
		featureFile = pFeatureFile;
		samplingMode = pSamplingMode;
		minSamples = pMinSamples;
		samplingCycles = pSamplingCycles;
	}
	
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
	
	private void checkSanity() {
		if ((!samplingMode.equals("rnd")) && (!samplingMode.equals("none"))) {
			System.err.println("Unknown Sampling Mode: "+samplingMode);
			System.exit(-1);
		}
		if (minSamples < 1) {
			System.err.println("Minimum Number of Samples < 1 is invalid");
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
		if (samplingCycles<1) {
			System.err.println("Number of Sampling Cycles < 1 is invalid");
			System.exit(-1);
		}
		int lFeatures = matrix[0].length;
		int lRows = matrix.length;
		if (index.length != lRows) {
			System.err.println("Number of rows of data matrix ("+lRows+") and index ("+index.length+") differ!");
			System.exit(-1);
		}
	}
	
	public void run() {
		PrintWriter lWriter = null;
		try {
			lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"));
			lWriter.println("Sampling SVM Study Report");
			lWriter.println("parameterFile: "+parameterFile.getAbsolutePath());
			lWriter.println("indexFile: "+indexFile.getAbsolutePath());
			lWriter.println("dataFile: "+dataFile.getAbsolutePath());
			lWriter.println("featureFile: "+featureFile.getAbsolutePath());
			checkFiles();
			parseIndex();
			parseFeatures();
			parseMatrix();
			parseParameters();
			checkSanity();
			prepareDirectories();
			Sample[] lSamples = new Sample[samplingCycles];
			for (elapsedCycles=0; elapsedCycles<lSamples.length; elapsedCycles++) {
				lSamples[elapsedCycles] = new Sample(samplingMode, minSamples, index);
			}
			ComputeSVMSampleThread[] lPool = new ComputeSVMSampleThread[threads];
			int lFinished = 0;
			for (elapsedCycles=0; elapsedCycles<lSamples.length; elapsedCycles++) {
				// Evaluate
				Sample lSample = lSamples[elapsedCycles];
				lSample.setIndexLabelsBestResults(null);
				int lID = -1;
				while (lID == -1) {
					for (int i=0; i<lPool.length; i++) {
						if (lPool[i] == null) {
							lID = i;
						}
						else if (lPool[i].getState() == Thread.State.TERMINATED) {
							lFinished++;
							System.out.println(lFinished+"/"+samplingCycles+" "+lPool[i].getSample().getFMeasure());
							lPool[i] = null;
							lID = i;
						}
						else {
							// Check Time
							if (timeout > 0) {
								if ((System.currentTimeMillis() - lPool[i].getStartTime()) > timeout) {
									// Kill Thread
									System.err.println("  Warning: Killing Thread because of Timeout "+(timeout/1000)+"s");
									lPool[i].kill();
									lPool[i].stop();
									lPool[i].getSample().setIndexLabelsBestResults(null);
									lPool[i] = null;
								}
							}
						}
					}
					if (lID == -1) {
						Thread.sleep(500);
					}
				}
				lPool[lID] = new ComputeSVMSampleThread(lSample, indexLabelParameters, index, indexLabels, matrix, tmpDirectory, lID);
				lPool[lID].start();
			}
			boolean lAllDone = false;
			while (!lAllDone) {
				lAllDone = true;
				for (int i=0; i<lPool.length; i++) {
					if (lPool[i] != null) {
						if (lPool[i].getState() == Thread.State.TERMINATED) {
							lFinished++;
							System.out.println(lFinished+"/"+samplingCycles+" "+lPool[i].getSample().getFMeasure());
							lPool[i] = null;								
						}
						else {			
							lAllDone = false;
							// Check Time
							if (timeout > 0) {									
								if ((System.currentTimeMillis() - lPool[i].getStartTime()) > timeout) {
									// Kill Thread
									System.err.println("  Warning: Killing Thread because of Timeout "+(timeout/1000)+"s");
									lPool[i].kill();
									lPool[i].stop();
									lPool[i].getSample().setIndexLabelsBestResults(null);
									lPool[i] = null;
								}
							}
						}
					}
				}
			}
			
			lWriter.println();
			double lMed = 0;
			int lMedCount = 0;
			for (int i=0; i<lSamples.length; i++) {
				if (lSamples[i].getIndexLabelsBestResults() != null) {
					lMed +=lSamples[i].getFMeasure();
					lMedCount++;
				}
			}
			lWriter.println("Average F-Measure: "+(lMed/lMedCount));
			System.out.println("Average F-Measure: "+(lMed/lMedCount));
			lWriter.flush();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (lWriter != null) lWriter.close();
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
				if (features.charAt(i) == '1') lCurrentLine.add(Double.parseDouble(lFields[i]));
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
		indexLabelParameters = new SVMParameters[indexLabels.length];
		while ((lLine = lReader.readLine()) != null) {
			lLine = lLine.trim();
			if (lLine.length() == 0) continue;
			if (lLine.startsWith("#")) continue;
			int lT = 0;
			double lC = 0;
			int lD = 0;
			double lG = 0;
			int lLabel = Integer.MAX_VALUE;
			String[] lFields = lLine.split(" ");
			for (int i=0; i<lFields.length; i++) {
				if (lLabel == Integer.MAX_VALUE) {
					try {
						lLabel = Integer.parseInt(lFields[i]);
					}
					catch (NumberFormatException e) {						
					}
				}
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
			for (int i=0; i<indexLabels.length; i++) {
				if (indexLabels[i] == lLabel) {
					indexLabelParameters[i] = new SVMParameters(lT, lC, lD, lG);
					break;
				}
			}			
		}
		lReader.close();
		System.out.println("Parsed Parameters");
		for (int i=0; i<indexLabelParameters.length; i++) {
			System.out.println("  "+indexLabelParameters[i].toString());
		}
	}
	
	private void parseFeatures() throws IOException {
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
	
	private void prepareDirectories() throws IOException {
		tmpDirectory.mkdirs();
		for (int i=0; i<threads; i++) {
			File lFile = new File(tmpDirectory.getAbsolutePath()+File.separator+Integer.toString(i));
			lFile.mkdirs();
		}
	}
	
}
