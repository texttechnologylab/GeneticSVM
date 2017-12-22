package org.geneticsvm;

import java.util.*;

public class Sample implements Comparable<Sample> {

	private String sample;
	private SVMResult[] indexLabelsBestResults;
	
	public Sample(String pSample) {
		sample = pSample;
	}
	
	public Sample(String pSamplingMode, int pSamplingMin, int[] pIndex) {
		if (pSamplingMode.equals("rnd")) {
			Map<Integer,List<Integer>> lMap = new HashMap<Integer,List<Integer>>();
			for (int i=0; i<pIndex.length; i++) {
				List<Integer> lList = lMap.get(pIndex[i]);
				if (lList == null) {
					lList = new Vector<Integer>();
					lMap.put(pIndex[i], lList);
				}
				lList.add(i);
			}
			for (List<Integer> lList:lMap.values()) {
				int lRemovableCount = Math.max(lList.size() - pSamplingMin, 0);
				int lRemoveCount = (int)Math.round(Math.random()*lRemovableCount);
				for (int i=0; i<lRemoveCount; i++) {
					lList.remove((int)Math.floor(Math.random()*lList.size()));
				}
			}
			StringBuffer lBuffer = new StringBuffer();
			for (int i=0; i<pIndex.length; i++) lBuffer.append('0');
			for (List<Integer> lList:lMap.values()) {
				for (Integer k:lList) {
					lBuffer.setCharAt(k, '1');
				}
			}
			sample = lBuffer.toString();
		}
		else {
			StringBuffer lBuffer = new StringBuffer();
			for (int i=0; i<pIndex.length; i++) lBuffer.append('1');
			sample = lBuffer.toString();
		}
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
		for (SVMResult lSVMResult:indexLabelsBestResults) {
			lResult += lSVMResult.getFMeasure();
		}
		return lResult/indexLabelsBestResults.length;
	}

	public boolean equals(Object o) {
		return sample.equals(o.toString());
	}
	
	public String getSample() {
		return sample;
	}
	
	public String toString() {
		return sample;
	}
	
	public int hashCode() {
		return sample.hashCode();
	}

	public int compareTo(Sample arg0) {
		Double l = new Double(getFMeasure());
		return l.compareTo(arg0.getFMeasure())*(-1);
	}
	
	public int getSampleCount() {
		int lResult = 0;
		for (int i=0; i<sample.length(); i++) {
			if (sample.charAt(i) == '1') lResult++;
		}
		return lResult;
	}
	
}
