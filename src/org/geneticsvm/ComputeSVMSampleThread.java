package org.geneticsvm;

import java.io.*;
import java.util.*;

public class ComputeSVMSampleThread extends Thread{
	
	private double[][] matrix;
	private int threadID;
	private File tmpDirectory;
	private int[] index;
	private int[] indexLabels;
	private Sample sample;
	SVMParameters[] indexLabelParameters;
	private long startTime;
	private Process process = null;
	
	public ComputeSVMSampleThread(Sample pSample, SVMParameters[] pIndexLabelParameters, int[] pIndex, int[] pIndexLabels, double[][] pMatrix, File pTmpDirectory, int pThreadID) {
		startTime = System.currentTimeMillis();
		sample = pSample;
		indexLabelParameters = pIndexLabelParameters;
		index = pIndex;
		indexLabels = pIndexLabels;
		matrix = pMatrix;
		tmpDirectory = pTmpDirectory;
		threadID = pThreadID;
	}
	
	public Sample getSample() {
		return sample;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public void kill() {
		if (process != null) {
			process.destroy();
		}
	}
	
	public void run() {
		SVMResult[] lIndexLabelsBestResults = new SVMResult[indexLabels.length];
		sample.setIndexLabelsBestResults(lIndexLabelsBestResults);
		for (int i=0; i<indexLabels.length; i++) {
			int lIndexLabel = indexLabels[i];
			lIndexLabelsBestResults[i] = new SVMResult(lIndexLabel, indexLabelParameters[i], -1, -1);
			// Write Training Data
			try {
				PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tmpDirectory.getAbsolutePath()+File.separator+Integer.toString(threadID)+File.separator+"train.svm"), "UTF-8"));					
				for (int k=0; k<matrix.length; k++) {
					if (sample.getSample().charAt(k) == '1') {
						lWriter.print(index[k] == lIndexLabel ? "1":"-1");
						for (int m=0; m<matrix[k].length; m++) {
							lWriter.print(" "+(m+1)+":"+matrix[k][m]);
						}
						lWriter.println();
					}
				}
				lWriter.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			// Start SVM
			String lSVMLearn = File.separatorChar=='/' ? "svm_learn" : "svm_learn.exe";
			try {
				List<String> lParamList = new Vector<String>();
				lParamList.add(lSVMLearn);
				for (String lString:indexLabelParameters[i].getParameterArray()) {
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
				BufferedReader lErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				BufferedReader lInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String lLine = null;
				while ((lLine = lErrorReader.readLine()) != null) {
					System.err.println(lLine);
				}
				double lPrecision = -1;
				double lRecall = -1;
				while ((lLine = lInputReader.readLine()) != null) {
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
				lErrorReader.close();
				lInputReader.close();
				try {
					if (process.waitFor() == 0) {
						lIndexLabelsBestResults[i].setPrecision(lPrecision);
						lIndexLabelsBestResults[i].setRecall(lRecall);
					}
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
