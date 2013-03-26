package edu.nyu.nlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * This is the main entry for the maximum entropy model.
 * @author liangzhou
 */
public class MaxEntExperiment {
    
    private static String _confFileName = null;
    private static Option _option = null;
    private static Mode _mode = Mode.NONE;
    
    private static enum Mode {
        NONE,
        TRAIN,
        TEST
    };  
    
    /**
     * Read in configuration file and properly set the internal parameters
     * for future usage.
     * @param args is the command line argument passed in from main().
     */
    private static void parseParamAndInit (String[] args) {
        // only expect one argument
        
        _confFileName = args[0];
        
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(_confFileName));
            
            // read in properties and initialize _option properly.
            _option = new Option();
            _option._dataDir = p.getProperty("dataDir", "").trim();
            _option._featureFileName = p.getProperty("featureFile", "").trim();
            _option._modelFile = p.getProperty("modelFile", "").trim();
            _option._rawTestFile = p.getProperty("rawTestData", "").trim();
            _option._testFile = p.getProperty("testData", "").trim();
            _option._rawTrainFile = p.getProperty("rawTrainData", "").trim();
            _option._trainFile = p.getProperty("trainData", "").trim();
            
            String mode = p.getProperty("mode");
            if (mode.toLowerCase().compareTo("test") == 0) { _mode = Mode.TEST; }
            else if (mode.toLowerCase().compareTo("train") == 0) { _mode = Mode.TRAIN; }
            else { _mode = Mode.NONE; }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }     
        
    }
    
    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        parseParamAndInit(args);        
        MaxEnt me = new MaxEnt(_option);
        
        switch (_mode) {
        case TRAIN:
            me.train();
            break;
        case TEST:
            me.test();
            System.out.println(me.performanceToString());
            break;
        default:
            System.err.println("Wrong mode configuration!");
        }
    }

}
