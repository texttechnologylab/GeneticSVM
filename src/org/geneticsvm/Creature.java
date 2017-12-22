package org.geneticsvm;

public class Creature implements Comparable<Creature> {

	private String features;
	private SVMResult[] indexLabelsBestResults;
	private boolean keep;
	
	public Creature(String pFeatures) {
		features = pFeatures;
	}
	
	public Creature(double pCompleteness, int pFeatureCount) {
		StringBuffer lBuffer = null;
		do {
			lBuffer = new StringBuffer();
			for (int i=0; i<pFeatureCount; i++) {
				if (Math.random()<pCompleteness) {
					lBuffer.append('1');
				}
				else {
					lBuffer.append('0');
				}
			}
		}
		while (lBuffer.indexOf("1") == -1);
		features = lBuffer.toString();
	}

	public boolean isKeep() {
		return keep;
	}

	public void setKeep(boolean keep) {
		this.keep = keep;
	}

	public void setIndexLabelsBestResults(SVMResult[] pIndexLabelsBestResults) {
		indexLabelsBestResults = pIndexLabelsBestResults;
	}
	
	public SVMResult[] getIndexLabelsBestResults() {
		return indexLabelsBestResults;
	}
	
	public double getFMeasure() {
		if (indexLabelsBestResults == null) return 0;
		double lResult = 0;
		try {
			for (SVMResult lSVMResult:indexLabelsBestResults) {
				lResult += lSVMResult.getFMeasure();
			}
			return lResult/indexLabelsBestResults.length;
		}
		catch (Exception e) {
			return 0;
		}
	}

	public boolean equals(Object o) {
		return features.equals(o.toString());
	}
	
	public String getFeatures() {
		return features;
	}
	
	public String toString() {
		return features;
	}
	
	public int hashCode() {
		return features.hashCode();
	}
	
	public Creature createOffspring(double pPermutationRate) {
		StringBuffer lBuffer = null;
		do {
			lBuffer = new StringBuffer();
			for (int i=0; i<features.length(); i++) {
				if (pPermutationRate > Math.random()) {
					if (features.charAt(i)=='0') {
						lBuffer.append('1');
					}
					else {
						lBuffer.append('0');
					}
				}
				else {
					lBuffer.append(features.charAt(i));
				}
			}
		} while (lBuffer.indexOf("1") == -1);
		return new Creature(lBuffer.toString());
	}

	public int compareTo(Creature arg0) {
		Double l = new Double(getFMeasure());
		return l.compareTo(arg0.getFMeasure())*(-1);
	}
	
	public int getFeaturesCount() {
		int lResult = 0;
		for (int i=0; i<features.length(); i++) {
			if (features.charAt(i) == '1') lResult++;
		}
		return lResult;
	}
	
}
