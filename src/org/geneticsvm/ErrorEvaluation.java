package org.geneticsvm;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class ErrorEvaluation {

    protected File indexFile;
    protected File matrixFile;
    protected Map<Integer, SVMParameters> indexLabelSVMParameterMap;
    protected String features;
    protected int[] index;
    protected double[][] matrix;
    protected TreeSet<Integer> indexSet;
    protected File tmpDirectory;

    public ErrorEvaluation(File pIndexFile, File pMatrixFile, Map<Integer, SVMParameters> pIndexLabelSVMParameterMap, String pFeatures, File pTmpDirectory) throws IOException {
        indexFile = pIndexFile;
        matrixFile = pMatrixFile;
        indexLabelSVMParameterMap = pIndexLabelSVMParameterMap;
        features = pFeatures;
        tmpDirectory = pTmpDirectory;
        initialize();
    }

    protected void initialize() throws IOException {
        {
            List<String[]> lFieldList = new ArrayList<>();
            BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(matrixFile), Charset.forName("UTF-8")));
            String lLine = null;
            while ((lLine = lReader.readLine()) != null) {
                lLine = lLine.trim();
                if (lLine.length() > 0) {
                    lFieldList.add(lLine.split("\t", -1));
                }
            }
            lReader.close();
            matrix = new double[lFieldList.size()][lFieldList.get(0).length];
            for (int i=0; i<lFieldList.size(); i++) {
                for (int k=0; k<lFieldList.get(i).length; k++) {
                    matrix[i][k] = Double.parseDouble(lFieldList.get(i)[k]);
                }
            }
        }
        index = new int[matrix.length];
        indexSet = new TreeSet<>();
        {
            BufferedReader lReader = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile), Charset.forName("UTF-8")));
            String lLine = null;
            int lID = 0;
            while ((lLine = lReader.readLine()) != null) {
                lLine = lLine.trim();
                if (lLine.length() > 0) {
                    index[lID] = Integer.parseInt(lLine);
                    indexSet.add(index[lID]);
                    lID++;
                }
            }
            lReader.close();
        }
    }

    public void evaluate(File pResultFile) throws IOException {
        PrintWriter lResultWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pResultFile), Charset.forName("UTF-8")));
        lResultWriter.println("Row-Index\tLabelIndex\tClassifyLabel\tExpected\tClassified\tSuccess");
        //lResultWriter.println(lID + "\t" + index[lID] + "\t" + lIndexLabel + "\t" + (lExpectedResult ? "1" : "0") + "\t" + (lClassifiedResult ? "1" : "0") + "\t" + (lExpectedResult == lClassifiedResult ? "1" : "0"));
        for (int lID = 0; lID < matrix.length; lID++) {
            for (int lIndexLabel:indexSet) {
                System.out.println("Processing "+lID+" - "+lIndexLabel);
                boolean lExpectedResult = index[lID] == lIndexLabel;
                boolean lClassifiedResult = false;
                boolean lPipelineExecuted = false;
                // OK- predict to classify lID with label lLabel based on leave-one-out
                File lTrainInputFile = new File(tmpDirectory.getAbsolutePath()+File.separator+"evaluate_train.svm");
                File lModelFile = new File(tmpDirectory.getAbsolutePath()+File.separator+"evaluate.model");
                File lPredictInputFile = new File(tmpDirectory.getAbsolutePath()+File.separator+"evaluate_predict.svm");
                File lResultFile = new File(tmpDirectory.getAbsolutePath()+File.separator+"evaluate.result");
                lResultFile.delete();
                lModelFile.delete();
                PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lTrainInputFile), "UTF-8"));
                PrintWriter lPredictWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(lPredictInputFile), Charset.forName("UTF-8")));
                for (int k=0; k<matrix.length; k++) {
                    // Skip Leave-one-out-line
                    if (k != lID) {
                        int lColID = 0;
                        lWriter.print(index[k] == lIndexLabel ? "1" : "-1");
                        for (int m = 0; m < matrix[k].length; m++) {
                            if (features.charAt(m) == '1') {
                                lColID++;
                                lWriter.print(" " + lColID + ":" + matrix[k][m]);
                            }
                        }
                        lWriter.println();
                    }
                    else {
                        int lColID = 0;
                        lPredictWriter.print("0");
                        for (int m = 0; m < matrix[k].length; m++) {
                            if (features.charAt(m) == '1') {
                                lColID++;
                                lPredictWriter.print(" " + lColID + ":" + matrix[k][m]);
                            }
                        }
                        lPredictWriter.println();
                    }
                }
                lWriter.close();
                lPredictWriter.close();
                // Start SVM
                String lSVMLearn = File.separatorChar=='/' ? "svm_learn" : "svm_learn.exe";
                String lSVMClassify = File.separatorChar=='/' ? "svm_classify" : "svm_classify.exe";
                BufferedReader lErrorReader = null;
                BufferedReader lInputReader = null;
                InputStream lInputStream = null;
                InputStream lErrorStream = null;
                OutputStream lOutputStream = null;
                try {
                    List<String> lParamList = new Vector<String>();
                    lParamList.add(lSVMLearn);
                    for (String lString:indexLabelSVMParameterMap.get(lIndexLabel).getParameterArray()) {
                        lParamList.add(lString);
                    }
                    lParamList.add("-x");
                    lParamList.add("0");
                    lParamList.add(lTrainInputFile.getAbsolutePath());
                    lParamList.add(lModelFile.getAbsolutePath());
                    String[] lParams = new String[lParamList.size()];
                    StringBuffer lDebug = new StringBuffer();
                    for (int k=0; k<lParams.length; k++) {
                        lDebug.append(lParamList.get(k)+" ");
                        lParams[k] = lParamList.get(k);
                    }
                    //System.out.println(lDebug.toString());
                    Process lProcess = Runtime.getRuntime().exec(lParams);
                    lInputStream = lProcess.getInputStream();
                    lErrorStream = lProcess.getErrorStream();
                    lOutputStream = lProcess.getOutputStream();
                    lErrorReader = new BufferedReader(new InputStreamReader(lErrorStream));
                    lInputReader = new BufferedReader(new InputStreamReader(lInputStream));
                    String lLine = null;
                    while ((lLine = lErrorReader.readLine()) != null) {
                        System.err.println(lLine);
                    }
                    double lPrecision = -1;
                    double lRecall = -1;
                    while ((lLine = lInputReader.readLine()) != null) {
                    }
                    lProcess.waitFor();
                    if (lModelFile.exists()) {
                        Process lPredictProcess = Runtime.getRuntime().exec(new String[]{lSVMClassify, lPredictInputFile.getAbsolutePath(), lModelFile.getAbsolutePath(), lResultFile.getAbsolutePath()});
                        BufferedReader lPredictInputReader = new BufferedReader(new InputStreamReader(lPredictProcess.getInputStream()));
                        while (lPredictInputReader.readLine() != null);
                        BufferedReader lPredictErrorReader = new BufferedReader(new InputStreamReader(lPredictProcess.getErrorStream()));
                        while (lPredictErrorReader.readLine() != null);
                        lPredictProcess.waitFor();
                        lPredictErrorReader.close();
                        lPredictInputReader.close();
                        if (lResultFile.exists()) {
                            BufferedReader lResultReader = new BufferedReader(new InputStreamReader(new FileInputStream(lResultFile)));
                            lLine = lResultReader.readLine();
                            if (Double.parseDouble(lLine) > 0) {
                                lClassifiedResult = true;
                                lPipelineExecuted = true;
                            }
                            else {
                                lClassifiedResult = false;
                                lPipelineExecuted = true;
                            }
                            lResultReader.close();
                        }
                        else {
                            System.err.println("Failed: No ResultFile");
                        }
                    }
                    else {
                        System.err.println("Failed: No ModelFile");
                    }
                }
                catch (Exception e) {
                    if ((!e.getMessage().equals("Stream closed")) && (!e.getMessage().equals("Bad file descriptor"))) {
                        e.printStackTrace();
                    }
                }
                finally {
                    if (lInputStream != null) {
                        try {
                            lInputStream.close();
                        }
                        catch (IOException e) {
                        }
                    }
                    if (lErrorStream != null) {
                        try {
                            lErrorStream.close();
                        }
                        catch (IOException e) {
                        }
                    }
                    if (lOutputStream != null) {
                        try {
                            lOutputStream.close();
                        }
                        catch (IOException e) {
                        }
                    }
                }
                if (lPipelineExecuted) {
                    lResultWriter.println(lID + "\t" + index[lID] + "\t" + lIndexLabel + "\t" + (lExpectedResult ? "1" : "0") + "\t" + (lClassifiedResult ? "1" : "0") + "\t" + (lExpectedResult == lClassifiedResult ? "1" : "0"));
                }
                else {
                    System.err.println("Pipeline Failed");
                }
            }
        }
        lResultWriter.close();
    }

    public static void main(String[] args) throws Exception {
        Map<Integer, SVMParameters> lMap = new HashMap<>();
        lMap.put(0, new SVMParameters(2, 10000, 0, 0.01));
        lMap.put(1, new SVMParameters(2, 100, 0, 0.1));
        lMap.put(2, new SVMParameters(2, 1000, 0, 0.01));
        ErrorEvaluation lErrorEvaluation = new ErrorEvaluation(new File("testdata/object.index"), new File("testdata/object.matrix"), lMap, "0100000111010100010010111110100111000010001010100100010010101", new File("tmp"));
        lErrorEvaluation.evaluate(new File("testdata/evaluation_results.txt"));
    }

}
