package org.geneticsvm;

import java.io.File;

public class SamplingSVM {

	public static void printSytnax() {
		System.out.println("Syntax: SamplingSVM -m <Sampling Mode> [-smin MinSamples] [-sc Sampling Cycles] -p <Parameter-File> -i <Index-File> -d <Data-File> -f <Feature-File> -o <output> [-tmp <Temp>] [-mt <Multithreading>] [-to timeout]");
		System.out.println("  -m Sampling Mode: none|rnd");
		System.out.println("  -smin Minimum Number of samples in a class (Default: 1)");
		System.out.println("  -sc Sampling Cycles (Default: 10)");
		System.out.println("  -p Parameter-File");
		System.out.println("  -i Index File");
		System.out.println("  -d Matrix Data File");
		System.out.println("  -f Feature File");
		System.out.println("  -o Output File");
		System.out.println("  -tmp Tmp-Directory (Default: tmp)");
		System.out.println("  -mt Multithreading (Default: Number of CPU Cores Threads)");
		System.out.println("  -to timeout for computation of a Thread in s (Default: 0 = no timeout)");
		System.exit(0);
	}
	
	public static void main(String[] args) {
		System.out.println("SamplingSVM 0.2.5");
		long lStart = System.currentTimeMillis();
		String lSamplingMode = null;
		int lMinSamples = 1;
		int lSamplingCycles = 10;
		File lParameterFile = null;
		File lIndexFile = null;
		File lDataFile = null;
		File lFeatureFile = null;
		File lOutputFile = null;
		File lTmpDirectory = new File(System.getProperty("user.dir")+File.separator+"tmp");
		int lThreads = Runtime.getRuntime().availableProcessors();
		long lTimeOut = 0;
		// Parse Parameters
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-m")) {
				lSamplingMode = args[++i];
			}
			else if (args[i].equals("-smin")) {
				lMinSamples = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-sc")) {
				lSamplingCycles = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-p")) {
				lParameterFile = new File(args[++i]);
			}
			else if (args[i].equals("-i")) {
				lIndexFile = new File(args[++i]);
			}
			else if (args[i].equals("-d")) {
				lDataFile = new File(args[++i]);
			}
			else if (args[i].equals("-f")) {
				lFeatureFile = new File(args[++i]);
			}
			else if (args[i].equals("-o")) {
				lOutputFile = new File(args[++i]);
			}
			else if (args[i].equals("-tmp")) {
				lTmpDirectory = new File(args[++i]);
			}
			else if (args[i].equals("-mt")) {
				lThreads = Integer.parseInt(args[++i]);
			}			
			else if (args[i].equals("-to")) {
				lTimeOut = Long.parseLong(args[++i]);
			}	
			else {
				printSytnax();
			}
		}
		//
		System.out.println("Using up to "+lThreads+" Threads ("+Runtime.getRuntime().availableProcessors()+" CPU Cores available)");
		SamplingSVMStudy lSamplingSVMStudy = new SamplingSVMStudy(lParameterFile, lIndexFile, lDataFile, lFeatureFile, lOutputFile, lTmpDirectory, lSamplingMode, lMinSamples, lSamplingCycles, lThreads, lTimeOut);
		lSamplingSVMStudy.start();
		while (lSamplingSVMStudy.getState() != Thread.State.TERMINATED) {
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("All Done - Operation took "+(System.currentTimeMillis()-lStart)+"ms");
	}

}
