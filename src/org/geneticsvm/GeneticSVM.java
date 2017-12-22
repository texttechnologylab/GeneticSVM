package org.geneticsvm;

import java.io.*;

public class GeneticSVM {

	public static void printSytnax() {
		System.out.println("Syntax: GeneticSVM -p <Parameter-File> -i <Index-File> -d <Data-File> [-f Feature File] -o <output> [-tmp <Temp>] [-m <maximumPopulation>] [-t <turns>] [-mt <Multithreading>] [-pr <Permutation Rate>] [-to timeout]");
		System.out.println("Syntax: GeneticSVM -r <Result-File> [-f Feature File] -o <output> [-tmp <Temp>] [-m <maximumPopulation>] [-t <turns>] [-mt <Multithreading>] [-pr <Permutation Rate>] [-to timeout]");
		System.out.println("  -r Resume from Result File. Replaces Parameters -p, -i, -d -f from file if not specified in command line");
		System.out.println("  -p Parameter-File");
		System.out.println("  -i Index File");
		System.out.println("  -d Matrix Data File");
		System.out.println("  -f Feature File (optional Restriction of Features)");
		System.out.println("  -o Output File");
		System.out.println("  -tmp Tmp-Directory (Default: tmp)");
		System.out.println("  -m Maximum Population (Default: 10)");
		System.out.println("  -t Number of Turns (Default: 10)");
		System.out.println("  -mt Multithreading (Default: Number of CPU Cores Threads)");
		System.out.println("  -pr Permutation Rate [0,1] (Default: 0.1)");
		System.out.println("  -to timeout for computation of a creature in s (Default: 0 = no timeout)");
		System.exit(0);
	}
	
	public static void main(String[] args) {
		System.out.println("GeneticSVM 0.2.5");
		long lStart = System.currentTimeMillis();
		File lParameterFile = null;
		File lIndexFile = null;
		File lDataFile = null;
		File lFeatureFile = null;
		File lResumeFile = null;
		File lOutputFile = null;
		File lTmpDirectory = new File(System.getProperty("user.dir")+File.separator+"tmp");
		int lMaxPopulation = 10;
		int lTurns = 10;
		int lThreads = Runtime.getRuntime().availableProcessors();
		long lTimeOut = 0;
		double lPermutationRate = 0.1;
		// Parse Parameters
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-p")) {
				lParameterFile = new File(args[++i]);
			}
			else if (args[i].equals("-i")) {
				lIndexFile = new File(args[++i]);
			}
			else if (args[i].equals("-d")) {
				lDataFile = new File(args[++i]);
			}
			else if (args[i].equals("-o")) {
				lOutputFile = new File(args[++i]);
			}
			else if (args[i].equals("-f")) {
				lFeatureFile = new File(args[++i]);
			}
			else if (args[i].equals("-r")) {
				lResumeFile = new File(args[++i]);
			}
			else if (args[i].equals("-tmp")) {
				lTmpDirectory = new File(args[++i]);
			}
			else if (args[i].equals("-m")) {
				lMaxPopulation = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-t")) {
				lTurns = Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-mt")) {
				lThreads = Integer.parseInt(args[++i]);
			}			
			else if (args[i].equals("-pr")) {
				lPermutationRate = Double.parseDouble(args[++i]);
			}
			else if (args[i].equals("-to")) {
				lTimeOut = Long.parseLong(args[++i]);
			}
			else if (args[i].equals("--help")) {
				printSytnax();
			}
			else if (args[i].equals("-h")) {
				printSytnax();
			}
			else {
				System.err.println("Unknown Parameter "+args[i]);
				System.err.println("Type 'GeneticSVM --help' for help");
			}
		}
		//
		System.out.println("Using up to "+lThreads+" Threads ("+Runtime.getRuntime().availableProcessors()+" CPU Cores available)");
		GeneticSVMStudy lGeneticSVMStudy = new GeneticSVMStudy(lParameterFile, lIndexFile, lDataFile, lFeatureFile, lResumeFile, lOutputFile, lTmpDirectory, lMaxPopulation, lTurns, lThreads, lPermutationRate, lTimeOut);
		lGeneticSVMStudy.start();
		while (lGeneticSVMStudy.getState() != Thread.State.TERMINATED) {
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
