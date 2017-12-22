package org.geneticsvm;

public class SVMResult implements Comparable<SVMResult> {

	private double precision;
	private double recall;
	private SVMParameters parameters;
	private int indexLabel;
	
	public SVMResult(int pIndexLabel, SVMParameters pParameters, double pPrecision, double pRecall)  {
		indexLabel = pIndexLabel;
		parameters = pParameters;
		precision = pPrecision;
		recall = pRecall;
	}
	
	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	public void setParameters(SVMParameters parameters) {
		this.parameters = parameters;
	}

	public SVMParameters getParameters() {
		return parameters;
	}

	public int getIndexLabel() {
		return indexLabel;
	}

	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getFMeasure() {
		return (precision >= 0) && (recall>=0) && (precision+recall>0) ? (2*precision*recall)/(precision+recall) : 0;
	}

	public int compareTo(SVMResult o) {
		return (new Double(getFMeasure())).compareTo(o.getFMeasure());
	}
	
}
