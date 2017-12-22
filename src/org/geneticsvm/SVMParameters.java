package org.geneticsvm;

public class SVMParameters {
	
	public static final int KERNEL_LIN = 0;
	public static final int KERNEL_POL = 1;
	public static final int KERNEL_RBF = 2;
	public static final int KERNEL_SIG = 3;

	protected int kernel;
	protected double tradeOff;
	protected int d;
	protected double gamma;
	
	protected SVMParameters(int pKernel, double pTradeOff, int pD, double pGamma) {
		kernel = pKernel;
		tradeOff = pTradeOff;
		d = pD;
		gamma = pGamma;
	}
	
	public static SVMParameters fromString(String pString) {
		if (pString.trim().equals("null")) return null;
		String[] lFields = pString.split(" ");
		int lKernel = 0;
		double lTradeOff = 0;
		int lD = 0;
		double lGamma = 0;
		for (int i=0; i<lFields.length; i++) {
			if (lFields[i].equals("-t")) {
				lKernel = Integer.parseInt(lFields[i+1]);
			}
			else if (lFields[i].equals("-c")) {
				lTradeOff = Double.parseDouble(lFields[i+1]);
			}
			else if (lFields[i].equals("-d")) {
				lD = Integer.parseInt(lFields[i+1]);
			}
			else if (lFields[i].equals("-g")) {
				lGamma = Double.parseDouble(lFields[i+1]);
			}
		}
		return new SVMParameters(lKernel, lTradeOff, lD, lGamma);
	}
	
	public static SVMParameters getLinearInstance(double pTrainingErrorTradeOff) {
		return new SVMParameters(KERNEL_LIN, pTrainingErrorTradeOff, 0, 0);
	}
	
	public static SVMParameters getPolynomialInstance(double pTrainingErrorTradeOff, int pD) {
		return new SVMParameters(KERNEL_LIN, pTrainingErrorTradeOff, pD, 0);
	}
	
	public static SVMParameters getRBFInstance(double pTrainingErrorTradeOff, int pGamma) {
		return new SVMParameters(KERNEL_LIN, pTrainingErrorTradeOff, 0, pGamma);
	}
	
	public static SVMParameters getSigmoidTanhInstance(double pTrainingErrorTradeOff) {
		return new SVMParameters(KERNEL_LIN, pTrainingErrorTradeOff, 0, 0);
	}
	
	public String toString() {
		switch (kernel) {
			case KERNEL_LIN: return "-t "+kernel+" -c "+tradeOff;
			case KERNEL_POL: return "-t "+kernel+" -c "+tradeOff+" -d "+d;
			case KERNEL_RBF: return "-t "+kernel+" -c "+tradeOff+" -g "+gamma;
			case KERNEL_SIG: return "-t "+kernel+" -c "+tradeOff;
			default: return "";
		}
	}
	
	public String[] getParameterArray() {
		return toString().split(" ");
	}
	
}
