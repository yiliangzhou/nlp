package edu.nyu.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import opennlp.maxent.*;
import opennlp.maxent.io.*;


public class MaxEnt {
    
    private Option _option;
    
    // The following fields are only intend to be used by testing functionality 
    // of MaxEnt instance.
    private int count = 0;
    private int error = 0;
    private int correctNounGroup = 0;
    private int totalKeys = 0;
    private int totalResponse = 0;
    private boolean tested = false;
    private List<String> expected = new ArrayList<String>();
    private List<String> predicts = new ArrayList<String>();
    private List<Word> sentence = new ArrayList<Word>();
    
    public MaxEnt (Option option) {
        this._option = option;
    }
    
    private String sentenceToFeatures (List<Word> sentence) {
        String entries = null;
        String[] features = {"prev", "curr", "next", "pre_pos", "curr_pos",
                "next_pos", "det_nn", "det_jj", "jj_nn", "nn_in"};
        for(int idx = 0; idx < sentence.size(); idx++) {           
            String[] values = new String[features.length];
            Word curr = sentence.get(idx);
            Word prev = (idx == 0) ? null : sentence.get(idx-1);
            Word next = (idx == sentence.size() - 1) ? null : sentence.get(idx+1);
            
            // fill out explicit context
            values[0] = (prev == null) ? "NA" :  prev.term;
            values[1] = curr.term;
            values[2] = (next == null) ? "NA" : next.term;
            values[3] = (prev == null) ? "NA" : prev.posTag;
            values[4] = curr.posTag;
            values[5] = (next == null) ? "NA" : next.posTag;
            
            // fill det_nn
            if (next != null && curr.posTag.equals("DT") && 
                    (next.posTag.equals("NN") || next.posTag.equals("NNP"))) { 
                values[6] = "TRUE";
            } else { values[6] ="FALSE"; }
            
            // fill det_jj
            if (next != null && curr.posTag.equals("DT") && 
                    next.posTag.equals("JJ")) { values[7] = "TRUE";
            } else { values[7] = "FALSE"; }
            
            // fill jj_nn
            if (next != null && curr.posTag.equals("JJ") && 
                    next.posTag.equals("NN")) { values[8] = "TRUE"; 
            } else { values[8] = "FALSE"; }
            
            // fill nn_in
            if (next != null && curr.posTag.equals("NN") && 
                    next.posTag.equals("IN")) { values[9] = "TRUE"; 
            } else { values[9] = "FALSE"; }
            
            for (int i = 0; i < features.length; i++) {
                entries += features[i] + "=" + values[i] + " ";
            }
            entries  += curr.output + "\n";
        }
        return entries;     
    }
    
    private void writeToDisk (FileOutputStream fos, List<Word> sentence) throws IOException {
        String entries = sentenceToFeatures(sentence);
        fos.write (entries.getBytes());
    }
    
    private void generateFeatureFile (String dataFolder, String rawDataFileName, 
            String processedDataFileName) {     
        // transfer raw training data into training data.
        try {
            FileInputStream fis = new FileInputStream(new File(dataFolder, rawDataFileName));
            Scanner raw = new Scanner(fis);
            FileOutputStream fos = new FileOutputStream(new File(dataFolder, processedDataFileName));
            
            List<Word> sentenceTmp = new ArrayList<Word>();
            while (raw.hasNextLine()) {
                String line = raw.nextLine();  
                if(!line.isEmpty()) {
                    String[] values = line.split("\\s+");
                    sentenceTmp.add(new Word(values[0].trim(), values[1].trim(), values[2].trim()));
                } else {
                    // write sentence out
                    writeToDisk(fos, sentenceTmp);
                    // clear sentence.
                    sentenceTmp.clear();
                }        
            }           
            if (!sentenceTmp.isEmpty()) {
                // write sentence out
                writeToDisk(fos, sentenceTmp);
                // clear sentence.
                sentenceTmp.clear();
            }        
            raw.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void train ( ) {
        String dataFolder = _option._dataDir;
        String rawDataFileName = _option._rawTrainFile;
        String dataFileName = _option._trainFile;
        String modelFileName = _option._modelFile;
        
        generateFeatureFile(dataFolder, rawDataFileName, dataFileName);   
        
        try {
            FileReader datafr = new FileReader(new File(dataFolder, dataFileName));
            EventStream es = new BasicEventStream(new PlainTextByLineDataStream(datafr));
            GISModel model = GIS.trainModel(es, 100, 4);
            File outputFile = new File(dataFolder, modelFileName);
            GISModelWriter writer = new SuffixSensitiveGISModelWriter(model, outputFile);
            writer.persist();
        } catch (Exception e) {
            System.out.print("Unable to create model due to exception: ");
            System.out.println(e);
        }
    }
    
    private int countMatchedNG (Set<NounGroup> e, Set<NounGroup> p) {
        int m = 0;
        for (NounGroup ng: p) { if (e.contains(ng)) { m++; } }
        return m;
    }
    
    private Set<NounGroup> getNounGroup (List<String> tags) {
        boolean inNounGroup = false;
        int start = -1;
        Set<NounGroup> ngs = new LinkedHashSet<NounGroup>();
        for(int i = 0; i < tags.size(); i++) {
            if (inNounGroup) {
                if (!tags.get(i).equals("I") && !tags.get(i).equals("B") ) {
                    ngs.add(new NounGroup(start, i-1));
                    inNounGroup = false;
                }                
                if (i != tags.size()-1) {
                    if ( (tags.get(i).equals("B") && tags.get(i+1).equals("O")) ||
                         (tags.get(i).equals("I") && tags.get(i+1).equals("O")) ) {
                        ngs.add(new NounGroup(start, i));
                        inNounGroup = false;
                    }
                    if (tags.get(i).equals("I") && tags.get(i+1).equals("B")  ||
                        tags.get(i).equals("B") && tags.get(i+1).equals("B")   ) {
                        ngs.add(new NounGroup(start, i));
                        start = i+1;
                    }
                } else {
                    if ( tags.get(i).equals("I") || tags.get(i).equals("B")) {
                        ngs.add(new NounGroup(start, i));
                        inNounGroup = false;
                    }
                }
            } else {
                // if we start a new noun group, record the current pos
                if ( (i==0 && tags.get(i).equals("I")) ||
                     (i != 0 && tags.get(i-1).equals("O") && tags.get(i).equals("I")) ||
                     (tags.get(i).equals("B")) ){
                    start = i;
                    inNounGroup = true;
                }
            }
        }
        if (inNounGroup) {
            ngs.add(new NounGroup(start, tags.size()-1));
        }
        return ngs;
    }
    
    private void evaluateSentence (GISModel m) {
        String entries = sentenceToFeatures(sentence);
        Scanner entrySc = new Scanner(entries);
        while (entrySc.hasNextLine()) {
            String featureEntry = entrySc.nextLine();
            String[] features = featureEntry.split("\\s+");
            expected.add(features[features.length-1]);
            features = Arrays.copyOf(features, features.length - 1);
            predicts.add(m.getBestOutcome(m.eval(features)));
        }
        
        // processing expected and predicts
        Set<NounGroup> expectedNG = getNounGroup(expected);
        totalKeys += expectedNG.size();
        Set<NounGroup> predictsNG = getNounGroup(predicts);
        totalResponse += predictsNG.size();
        correctNounGroup += countMatchedNG(expectedNG, predictsNG);
        
        for(int i = 0; i < predicts.size(); i++) {
            if (predicts.get(i).compareTo(expected.get(i)) != 0) {
                error++;
            }
            count++;
        }     
        expected.clear();
        predicts.clear();
        sentence.clear();
    }
    
    public String performanceToString() {
        if (!tested) { return "No test data..."; }
        double precision = (double) correctNounGroup / totalResponse;
        double recall = (double) correctNounGroup / totalKeys;
        double fMeasure = 2.0 / ((1.0/recall) + (1.0/precision));
        double ar = (double)(count-error)/count * 100;
        String ret = String.format("Total %d predicts, accuracy rate is: %.2f%%", count, ar);
        ret += String.format(" Recall: %.2f%%", recall*100);
        ret += String.format(" Precision: %.2f%%", precision*100); 
        ret += String.format(" F measure: %.2f%%", fMeasure*100); 
        return ret;
    }
    
    public void test ( ) {
        GISModel m;        
        try {
            m = new SuffixSensitiveGISModelReader(
                    new File(_option._dataDir, _option._modelFile)).getModel();           
            FileInputStream fis = new FileInputStream(
                    new File(_option._dataDir, _option._rawTestFile));
            Scanner sc = new Scanner(fis);
            while(sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isEmpty() && !sentence.isEmpty()) {
                    evaluateSentence(m);                 
                    continue;
                } else {
                    String[] values = line.split("\\s+");
                    sentence.add(new Word(values[0], values[1], values[2]));
                }
            }
            if(!sentence.isEmpty()) {
                evaluateSentence(m);
            }
            
            tested = true;
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }      
    }    
}
