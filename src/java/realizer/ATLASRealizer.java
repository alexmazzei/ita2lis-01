package realizer;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ListIterator;
import java.util.Vector;
import java.util.Properties;
import java.util.ArrayList;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.Document;


import opennlp.ccg.*;
import opennlp.ccg.grammar.Grammar;
import opennlp.ccg.realize.Edge;
import opennlp.ccg.realize.Realizer;
import opennlp.ccg.synsem.LF;
import opennlp.ccg.ngrams.StandardNgramModel;
import opennlp.ccg.ngrams.FactoredNgramModelFamily;

import opennlp.ccg.synsem.Sign;
import opennlp.ccg.parse.DerivationHistory;

import opennlp.ccg.util.Visualizer;

import java.io.*;
import java.net.*;
import java.io.IOException;


public class ATLASRealizer {
    private Grammar CCGgrammar;
    private Realizer CCGrealizer;
    private String dynFilesPath;
    private String staFilesPath;
    private String donnaPath;
    private String languageModelFile;
    private String testInputFile;

    private  derivationContainer dc;

    public ATLASRealizer() {
        //INITIALIZATION
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("./resources/atlas.ini"));
            dynFilesPath = props.getProperty("dynamicFilesPath");
            staFilesPath = props.getProperty("staticFilesPath");
	    testInputFile = props.getProperty("testInputFile");
	    languageModelFile = props.getProperty("langModFile");
	    donnaPath = props.getProperty("donnaPath");
            //System.out.println("CCG = " + props.getProperty("grammarGeneration"));
	    //System.out.println("Model of Language = " + languageModelFile);
	    //System.out.println("Input File Test = " + staFilesPath + testInputFile);
            //System.out.println("dynamicFilesPath = " + props.getProperty("dynamicFilesPath"));
            //props.list(System.out);

        } catch (IOException ex) {
	    Logger.getLogger(ATLASRealizer.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Frame Input Generatore
        //Frame AEWLIS
        try {//Initiaization CCGRealizer
             //String grammarFile = "/Users/mazzei/lavori/Projects/ATLAS/softExt/openccg/ccg-format-grammars/lis/meteo19/meteo19-NotteRicercatori-01-grammar.xml";
	    String grammarFile = props.getProperty("grammarGeneration");
	    URL    grammarURL =new File(grammarFile).toURI().toURL();
	    //             System.out.println("string grammar = "+ grammarFile +  " url grammar= " + grammarURL.toString());
	    CCGgrammar  = new Grammar(grammarURL);
	    CCGrealizer = new Realizer(CCGgrammar);


	    //langModFile=/Users/mazzei/lavori/Projects/ATLAS/softwareMazzei/generatoreAtlas/lispSentenceDesigner/data-files-tmp/2012-01-16/n.2bo
	    //--> modello basato sui bigrammi di glosse
	    //CCGrealizer.signScorer = new StandardNgramModel(2,languageModelFile);

	    //langModFile=/Users/mazzei/lavori/Projects/ATLAS/softwareMazzei/generatoreAtlas/lispSentenceDesigner/data-files-tmp/2012-01-16/spec-02.flm
	    //--> modello basato sui trigrammi e bigrammi di glosse e semantic ontology class
	    CCGrealizer.signScorer = new FactoredNgramModelFamily(languageModelFile,true);

        } catch (Exception ex) {
            Logger.getLogger(ATLASRealizer.class.getName()).log(Level.SEVERE, null, ex);
        }



    }

    private  String readFileAsString(String filePath) throws java.io.IOException{
	byte[] buffer = new byte[(int) new File(filePath).length()];
	BufferedInputStream f = null;
	try {
	    f = new BufferedInputStream(new FileInputStream(filePath));
	    f.read(buffer);
	} finally {
	    if (f != null) try { f.close(); } catch (IOException ignored) { }
	}
	f.close();
	return new String(buffer);
    }

    public String GetRealization(String filePath, String xmlString) throws java.io.IOException{
        String result = "";
        //String  inputFile = "/Users/mazzei/lavori/Projects/ATLAS/softExt/openccg/ccg-format-grammars/lis/meteo19/input-generation-01.xml";
        String  inputFile = filePath;
        URL     inputURL = new File(inputFile).toURI().toURL();
        SAXBuilder builder = new SAXBuilder();
        try {
	    //Document inputDocRealizer  = builder.build(inputURL);
	    Document inputDocRealizer  = builder.build(new StringReader(xmlString));
	    LF lf = CCGrealizer.getLfFromDoc(inputDocRealizer);
	    //Edge bestEdge = CCGrealizer.realize(lf);
            Edge bestEdge = CCGrealizer.realize(lf,null,300,true);


	    result = bestEdge.getSign().getOrthography() + "\n"
		+ bestEdge.getSign().getBracketedString() + "\n"
		+ bestEdge.getSign().toString() + "\n--\n";

	    DerivationHistory dh = bestEdge.getSign().getDerivationHistory();
	    //System.out.println(dh.toString());

	    //Begin TeX derivation
	    /*Visualizer vis = new Visualizer();
	    String fn = "./tmp/tmp-der.tex";//vis.getTempFileName();
	    vis.saveTeXFile(bestEdge.getSign(),fn);*/
	    //End   TeX derivation

	    dc = new derivationContainer(bestEdge.getSign());
        } catch (JDOMException ex) {
            Logger.getLogger(ATLASRealizer.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }



    public String trialRealization(String xmlString) {
	String outGen = "";
	try
	    {
		outGen = this.GetRealization(staFilesPath + testInputFile,xmlString);
		//outGen = this.GetRealization("/Users/mazzei/lavori/Projects/ATLAS/softExt/nunnariev/ATLASPlayer-ReleaseMac-110915-fnunnari/playable_files/tmp-outRealizer-00.xml");
	    }
	catch (IOException ex) {
	    Logger.getLogger(ATLASRealizer.class.getName()).log(Level.SEVERE, null, ex);
	};

	//	System.out.println(dc.toDonna());
	//System.out.println(dc.toAVM_TUT());

	//return outGen + "\n dc.toString()= " +  dc.toString() + "\n dc.toAEWLIS()= " + dc.toAEWLIS() + "\n dc.toAVM_TUT()= " + dc.toAVM_TUT();
        //return outGen + "\n" + dc.toAEWLIS();
        return dc.toAEWLIS();
    }



    private  void serverMode() throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(4444);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 4444.");
            System.exit(1);
        }
        Socket clientSocket = null;
	try {

	    while (true){
	    try {
		clientSocket = serverSocket.accept();
	    } catch (IOException e) {
		System.err.println("Accept failed.");
		System.exit(1);
	    }

	    String inputLine="";
	    String outputLine="";

	    BufferedReader in = new BufferedReader(
						   new InputStreamReader(
									 clientSocket.getInputStream()));
	    boolean test = true;//in.ready();
	    while (test) {
		inputLine += in.readLine() + "\n";
		test = in.ready();
 	    }
	    //System.out.println("fuori");
	    System.out.println(">>>>>>>>> INREALIZER");
	    System.out.println(inputLine);
	    System.out.println("<<<<<<<<<");
	    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	    /*	    outputLine = "hey";
	    out.println(outputLine);
	    out.println(inputLine);*/

	    try{
		// Create file
		//		FileWriter fstream = new FileWriter("/Users/mazzei/lavori/Projects/ATLAS/softExt/nunnariev/ATLASPlayer-ReleaseMac-110915-fnunnari/playable_files/tmp-inRealizer-00.xml");
		FileWriter fstream = new FileWriter( dynFilesPath + "tmp-inRealizer-00.xml");
		BufferedWriter outFile = new BufferedWriter(fstream);
		outFile.write(inputLine);
		//Close the output stream
		outFile.close();
	    }catch (Exception e){//Catch exception if any
		System.err.println("Error: " + e.getMessage());
	    }

	    String outGen = "";
	    try
		{
		    outGen = this.GetRealization(dynFilesPath + "tmp-inRealizer-00.xml","");
		}
	    catch (Exception e) {
		System.err.println("Error: " + e.getMessage());
		clientSocket.close();
	    };

	    try
		{
		    //System.out.println("DEBUG:: tut format:\n"+ dc.toTUT());
		    outputLine = dc.toAVM_TUT();
		}
	    catch (Exception e) {
		throw new Exception("Error in: dc.toAVM_TUT()");
	    };

	    out.println(outputLine);
	    //	    out.println(inputLine);



	    out.flush();
	    out.close();
	    in.close();
	    clientSocket.close();

	    //System.out.println(dc.toDonna());
	    System.out.println(dc.toString());
	    System.out.println(dc.toAEWLIS());
	    System.out.println(dc.toAVM_TUT());




	    }
	}
	    catch (Exception e) {
		System.out.println("Error in method: serverMode(); " + e.getMessage());
		System.out.println("REALIZER-ERROR");
	    }
	    //serverSocket.close();
    }

    public static void tryRealizer() {
        System.out.println("GOGOGOGOGOGO!!!!");
        //     return "I like it!";
    }


    public String tryRealizer2() {
        //System.out.println("GOGOGOGOGOGO!!!!");
        return "I like it!";
    }




    public static void main(String args[]) {

	//new ATLASRealizer.go();
	ATLASRealizer real = new ATLASRealizer();
	//System.out.println(real.trialRealization());

		try{
		    real.serverMode();
		} catch (Exception e) {
		    System.out.println("Message 2"+e.getMessage());
		    System.out.println("REALIZER-ERROR");
		}

	    //real.trialRealization();

    }


}
