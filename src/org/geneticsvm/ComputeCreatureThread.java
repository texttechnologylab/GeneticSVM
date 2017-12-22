package org.geneticsvm;

import java.io.*;
import java.util.*;

public class ComputeCreatureThread extends Thread {

	private Creature creature;
	private List<SVMParameters> parameters;
	private int[] index;
	private double[][] matrix;
	private int threadID;
	private File tmpDirectory;
	private int[] indexLabels;
	private SVMResult[] indexLabelsBestResults;
	private long startTime;
	private Process process = null;
	private InputStream inputStream;
	private InputStream errorStream;
	private OutputStream outputStream;
	private long timeout;
	private SVMResult[] oldIndexLabelsBestResults;
	
	public ComputeCreatureThread(Creature pCreature, List<SVMParameters> pParameters, int[] pIndex, int[] pIndexLabels, double[][] pMatrix, File pTmpDirectory, int pThreadID, long pTimeout) {
		startTime = System.currentTimeMillis();
		creature = pCreature;
		parameters = pParameters;
		index = pIndex;
		indexLabels = pIndexLabels;
		matrix = pMatrix;
		tmpDirectory = pTmpDirectory;
		threadID = pThreadID;
		timeout = pTimeout;
		oldIndexLabelsBestResults = pCreature.getIndexLabelsBestResults();		
	}
	
	public Creature getCreatue() {
		return creature;
	}
	
	
	public boolean isTimeout() {
		if (timeout == 0) return false;
		boolean lResult = System.currentTimeMillis()-startTime > timeout;
		return lResult;
	}
	
	public void kill() {
		if (process != null) {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				}
				catch (IOException e) {
				}
			}
			if (errorStream != null) {
				try {
					errorStream.close();
				}
				catch (IOException e) {
				}
			}
			process.destroy();
		}
	}
	
	public void run() {
		creature.setKeep(false);
		// Evaluate Parameters
		indexLabelsBestResults = new SVMResult[indexLabels.length];
		for (int i=0; i<indexLabelsBestResults.length; i++) {
			indexLabelsBestResults[i] = new SVMResult(indexLabels[i], null, -1, -1);
		}
		for (SVMParameters lParameters:parameters) {
			if (isTimeout()) {
				indexLabelsBestResults = null;
				break;
			}
			for (int i=0; i<indexLabels.length; i++) {
				if (isTimeout()) {
					indexLabelsBestResults = null;
					break;
				}
				int lIndexLabel = indexLabels[i];
				// Write Training Data
				try {
					PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tmpDirectory.getAbsolutePath()+File.separator+Integer.toString(threadID)+File.separator+"train.svm"), "UTF-8"));					
					for (int k=0; k<matrix.length; k++) {
						int lColID = 0;
						lWriter.print(index[k] == lIndexLabel ? "1":"-1");
						for (int m=0; m<matrix[k].length; m++) {
							if (creature.getFeatures().charAt(m)=='1') {
								lColID++;
								lWriter.print(" "+lColID+":"+matrix[k][m]);
							}
						}
						lWriter.println();
					}
					lWriter.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				// Start SVM
				String lSVMLearn = File.separatorChar=='/' ? "svm_learn" : "svm_learn.exe";
				BufferedReader lErrorReader = null;
				BufferedReader lInputReader = null;
				try {
					List<String> lParamList = new Vector<String>();
					lParamList.add(lSVMLearn);
					for (String lString:lParameters.getParameterArray()) {
						lParamList.add(lString);
					}
					lParamList.add("-x");
					lParamList.add("1");
					lParamList.add(tmpDirectory.getAbsolutePath()+File.separator+Integer.toString(threadID)+File.separator+"train.svm");
					lParamList.add(tmpDirectory.getAbsolutePath()+File.separator+Integer.toString(threadID)+File.separator+"train.model");
					String[] lParams = new String[lParamList.size()];
					StringBuffer lDebug = new StringBuffer();
					for (int k=0; k<lParams.length; k++) {
						lDebug.append(lParamList.get(k)+" ");
						lParams[k] = lParamList.get(k);
					}
					//System.out.println(lDebug.toString());
					process = Runtime.getRuntime().exec(lParams);
					inputStream = process.getInputStream();
					errorStream = process.getErrorStream();
					outputStream = process.getOutputStream();
					lErrorReader = new BufferedReader(new InputStreamReader(errorStream));
					lInputReader = new BufferedReader(new InputStreamReader(inputStream));
					String lLine = null;
					while ((lLine = lErrorReader.readLine()) != null) {
						System.err.println(lLine);
						if (isTimeout()) {
							indexLabelsBestResults = null;
							break;
						}
					}
					double lPrecision = -1;
					double lRecall = -1;
					while ((lLine = lInputReader.readLine()) != null) {
						if (isTimeout()) {
							indexLabelsBestResults = null;
							break;
						}
						if (lLine.startsWith("Leave-one-out estimate of the recall")) {
							try {
								lRecall = Double.parseDouble(lLine.substring(lLine.indexOf("=")+1, lLine.indexOf("%")));
							}
							catch (NumberFormatException f) {
								lRecall = -1;
							}
						}
						if (lLine.startsWith("Leave-one-out estimate of the precision")) {
							try {
								lPrecision = Double.parseDouble(lLine.substring(lLine.indexOf("=")+1, lLine.indexOf("%")));
							}
							catch (NumberFormatException f) {
								lPrecision = -1;
							}
						}
						//System.out.println(lLine);
					}
					if (!isTimeout()) {
						try {
							if (process.waitFor() == 0) {
								if ((lPrecision >= 0) && (lRecall >= 0)) {
									double lFMeasure = 0;								
									if (lPrecision+lRecall > 0) {
										lFMeasure = (2 *  lPrecision * lRecall) / (lPrecision + lRecall);
										if ((lFMeasure > indexLabelsBestResults[i].getFMeasure()) || (indexLabelsBestResults[i].getParameters()==null)) {
											indexLabelsBestResults[i].setParameters(lParameters);
											indexLabelsBestResults[i].setPrecision(lPrecision);
											indexLabelsBestResults[i].setRecall(lRecall);
										}
									}
								}
							}
							else {
								throw new Exception("SVM exited with error");
							}
						}
						catch (InterruptedException e) {
							if (isTimeout()) {
								indexLabelsBestResults = null;
								break;
							}
						}
					}
				}
				catch (Exception e) {
					if ((!e.getMessage().equals("Stream closed")) && (!e.getMessage().equals("Bad file descriptor"))) {
						e.printStackTrace();
					}
				}
				finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						}
						catch (IOException e) {							
						}
					}
					if (errorStream != null) {
						try {
							errorStream.close();
						}
						catch (IOException e) {							
						}
					}
					if (outputStream != null) {
						try {
							outputStream.close();
						}
						catch (IOException e) {							
						}
					}
				}
			}
		}
		if ((!isTimeout()) || (creature.getFMeasure()==0)) {
			creature.setIndexLabelsBestResults(indexLabelsBestResults);
		}
		else {
			if (creature.getFMeasure()>0) {
				creature.setIndexLabelsBestResults(oldIndexLabelsBestResults);
				creature.setKeep(true);
			}
		}
	}
	
}
