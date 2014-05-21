package realizer;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Iterator;
import java.util.List;

import opennlp.ccg.synsem.Sign;
import opennlp.ccg.lexicon.Word;
import opennlp.ccg.parse.DerivationHistory;
import opennlp.ccg.util.Pair;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.io.*;
/**
 *
 * @author mazzei
 */
public class derivationContainer {
    private Sign sign;
    private ArrayList<tokenAWLIS>  tokens;

    public derivationContainer (Sign s)
    {
	sign = s;
	tokens = new ArrayList<tokenAWLIS>();
	fillTokens(s);

	for (int i = 0; i < tokens.size(); i++) {
	    tokens.get(i).setNumber(i+1);
	}

	for (int i = 0; i < tokens.size(); i++) {
	    for (int j = 0; j < tokens.size(); j++) {
		if(tokens.get(i).getNominalParent().equals(tokens.get(j).getNominal()))
		    tokens.get(i).setNumberParent(j+1);
	    }
	}

	this.modifySpecialRelationsInTree();
	//printDerivation();
    }

    private void fillTokens(Sign ssign) {
	if (ssign.getDerivationHistory().getInputs() == null)
	    {
		String word = ssign.getWords().iterator().next().getForm(); //ssign.getOrthography();
		String lem  = ssign.getWords().iterator().next().getStem();  //extractLemma(ssign.getOrthography());
		String pos = "VnA";
		if(ssign.getCategory().getTarget().getFeatureStructure().hasAttribute("PoS")) {pos = ssign.getCategory().getTarget().getFeatureStructure().getValue("PoS") + "";}//String pos  = extractPoS(ssign.getOrthography());
		//String pos = ssign.getPOS();

		String posX = "VnA";
		if(ssign.getCategory().getTarget().getFeatureStructure().hasAttribute("position_x")) {posX = ssign.getCategory().getTarget().getFeatureStructure().getValue("position_x") + "";}
		String posY = "VnA";
		if(ssign.getCategory().getTarget().getFeatureStructure().hasAttribute("position_y")) {posY = ssign.getCategory().getTarget().getFeatureStructure().getValue("position_y") + "";}
		String posZ = "VnA";
		if(ssign.getCategory().getTarget().getFeatureStructure().hasAttribute("position_z")) {posZ = ssign.getCategory().getTarget().getFeatureStructure().getValue("position_z") + "";}


		Boolean pl  = false;//"VnA";//extractPlural(ssign.getOrthography());
		if(ssign.getCategory().getTarget().getFeatureStructure().hasAttribute("num")) {pl = (ssign.getCategory().getTarget().getFeatureStructure().getValue("num") + "" == "plur");}//String pos  = extractPoS(ssign.getOrthography());
		String nom  = extractNominals(ssign.getCategory().getLF().toString());
		String nop  = extractNominalParent(nom,sign.getCategory().getLF().toString());
		String rel  = extractRelation(nom,sign.getCategory().getLF().toString());

		tokenAWLIS tok = new tokenAWLIS(
						word,
						lem,//ssign.getOrthography(),//lemma
						pos,//ssign.getOrthography(),//PoS
						posX,
						posY,
						posZ,
						pl,//false, //plural
						nom, //nominal
						0, //number
						nop, //nominalParent
						0, //numberParent
						rel //semanticRole
						);
		tokens.add(tok);

// 		System.out.println("Category!! " + ssign.getCategory().getTarget().getFeatureStructure() +"\n");
// 		System.out.println("Attributes!! " + ssign.getCategory().getTarget().getFeatureStructure().getAttributes()  +"\n");
// 		System.out.println("PoS!! " + ssign.getCategory().getTarget().getFeatureStructure().getValue("PoS") +"\n");

	    }
	else
	    {
		Sign[] inputs = ssign.getDerivationHistory().getInputs();
		for (int i = 0; i < inputs.length; i++) {
		    this.fillTokens(inputs[i]);
		}
	    }
    }



    private String extractLemma(String word) {
	Pattern pattern = Pattern.compile("(^[^_]+)_");
	Matcher matcher = pattern.matcher(word);

	StringBuffer sb = new StringBuffer();
	while (matcher.find()) {
	    sb.append(matcher.group(1) );
	}
	return sb.toString();
    }

    private String extractPoS(String word) {
	Pattern pattern = Pattern.compile("^[^_]+_([^_]+)_");
	Matcher matcher = pattern.matcher(word);

	StringBuffer sb = new StringBuffer();
	while (matcher.find()) {
	    sb.append(matcher.group(1) );
	}
	return sb.toString();


    }

    private Boolean extractPlural(String word) {
	Pattern pattern = Pattern.compile("_pl");
	Matcher matcher = pattern.matcher(word);

	StringBuffer sb = new StringBuffer();
	while (matcher.find()) {
	    sb.append(matcher.group(1) );
	}
	if(sb.toString().isEmpty())
	    return false;
	else
	    return true;
    }

    private String extractNominals(String lfInput) {
	// This function search for "dominant" nominals in the logical form.
	// Moreover the functions accounts too for special case
	// deriving from functional lexical items. These items do not
	// have a lexical semamntics, so they do not have a logical
	// nominal. In order to create "ad hoc" relations for these
	// items, (1) the function "extractNominals(LF)" assign the
	// same nominal of the brother and later (2) the function
	// "modifySpecialRelationsInTree()" modifies the tree by
	// changing the "numberParent", "nominalParent", "relation" of
	// the brother: at the the end the brother will be the child
	// of the functional lexical item.

	Pattern pattern = Pattern.compile("@(.\\d+)[^\\(]+\\([^<]");
	Matcher matcher = pattern.matcher(lfInput);

	StringBuffer sb = new StringBuffer();
	while (matcher.find()) {
	    sb.append(matcher.group(1) );
	}

	if(sb.length() == 0)
	    {
		//20120202: workaround for relative
		//clause that do not have a proper
		//nominal: we use the nominal of the
		//relation argument, that is the
		//"brother" of the lexical item;

		String specialRelation = "";
		if(lfInput.indexOf("RELCL") != -1)
		    {specialRelation = "RELCL";}

		pattern = Pattern.compile(specialRelation + ">(.\\d+)");
		matcher = pattern.matcher(lfInput);
		while (matcher.find()) {
		    sb.append(matcher.group(1) );
		}
	    }

	//System.out.println("DEBUG:: extractNominals ->  LF=" + lfInput  + "- Nominal(s)=" + sb.toString());
	return sb.toString();

    }

    private String extractNominalParent(String n, String lfInput) {
	String p = "@(.\\d+)[^\\(]+\\(<[^>]+>" + n ;
	Pattern pattern = Pattern.compile(p);
	Matcher matcher = pattern.matcher(lfInput);

	StringBuffer sb = new StringBuffer();
	while (matcher.find()) {
	    sb.append( matcher.group(1) );
	}
	//System.out.println("DEBUG:: extractNominalParent ->Path=" + p + " - " + "LF=" + lfInput  + "- NominalParent=" + sb.toString());

	if(sb.toString().isEmpty()) return "T0";
	return sb.toString();
    }

    private String extractRelation(String n, String lfInput) {
	String p = "@.\\d+[^\\(]+\\(<([^>]+)>" + n ;
	Pattern pattern = Pattern.compile(p);
	Matcher matcher = pattern.matcher(lfInput);



	StringBuffer sb = new StringBuffer();
	while (matcher.find()) {
	    sb.append(matcher.group(1) );
	}
	if(sb.toString().isEmpty()) return "TOP";
	//System.out.println("DEBUG:: Nominal "+ n +" - Path=" + p + " - " + "LF=" + lfInput  + "- NominalParent=" + sb.toString());
	//System.out.println("DEBUG:: extractRelation=" + sb.toString());
	return sb.toString();
    }


    private void modifySpecialRelationsInTree() {
	// This function accounts for special case deriving from
	// functional lexical items. These items do not have a lexical
	// semamntics, so they do not have a logical nominal. In order
	// to create "ad hoc" relations for these items, (1) the
	// function "extractNominals(LF)" assign the same nominal of
	// the brother and later (2) the function
	// "modifySpecialRelationsInTree()" modifies the tree by
	// changing the "numberParent", "nominalParent", "relation" of
	// the brother: at the the end the brother will be the child
	// of the functional lexical item.
	for (int i = 0; i < tokens.size(); i++) {
	    /*CASE 1: Special account for the relation SYN-RMOD+RELCL
	       IF there is a pronoun related with SYN-RMOD+RELCL
	          AND
	          there is a (non-pronoun) "brother" related with SYN-RMOD+RELCL
	       THEN
	          the brother becames a child of the pronoun by using the "SYN-ARG" relation*/
	    if(tokens.get(i).getSemanticRole().equals((String) "SYN-RMOD+RELCL") &&
	       tokens.get(i).getPos().equals((String) "pron")
	       )
		{
		    //System.out.println("DEBUG: modifySpecialRelationsInTree() -> index =" + i);
		    for (int brother = 0; brother < tokens.size(); brother++) {
			if(tokens.get(brother).getSemanticRole().equals((String) "SYN-RMOD+RELCL") &&
			   !tokens.get(brother).getPos().equals((String) "pron")
			   )
			    {
				//System.out.println("DEBUG: modifySpecialRelationsInTree() -> brother =" + brother);
				tokens.get(brother).resetParent(tokens.get(i).getNumber(),
								tokens.get(i).getNominal(),
								"SYN-ARG");
			    }
		    }
		}
	}
    }



    public String toAVM_TUT() {
	int index = 0;
	for (int i = 0; i < tokens.size(); i++) {
	    //System.out.println("DEBUG: parent=" + tokens.get(i).getNumberParent());
	    if(tokens.get(i).getNumberParent() == 0)
		{
		    index = i;
		}
	}
	//System.out.println("DEBUG: index root=" + index);

	return toAVM_TUT(index);
    }

    public String toAVM_TUT(int i) {
	String result = "";
	result = "((head ((form |" + tokens.get(i).getRealLemma() + "|) (position " + (i+1) + ") (idSign " + tokens.get(i).getAtlasId() + ") (syn ((lemma " +  tokens.get(i).getRealLemma() + ") (cat " +   tokens.get(i).getPos() + ") (number ";
	if(tokens.get(i).getPlural())
	    { result += "plur";}
	else
	    { result += "sing";}
	result += ") (arg-ref 0) (hand " + tokens.get(i).getHandsNumber() + "))) (link ";

	String link = tokens.get(i).getSemanticRole();

	if(!link.equals("TOP"))
	    {
		link = link.substring(4);
	    }

	String cat = tokens.get(i).getPos();
	//	System.out.println("DEBUG: posEl=" + cat + " i=" +  i);

	if( (link.indexOf("SUBJ") != -1) ||
	    (link.indexOf("OBJ") != -1)  ||
	    (link.indexOf("ARG") != -1)    )
	    {
		//		System.out.println("DEBUG: posEl=" + cat + " posParent=" +  tokens.get(tokens.get(i).getNumberParent()-1).getPos());
		cat = tokens.get(tokens.get(i).getNumberParent()-1).getPos();
	    }
	if(!cat.equals("VnA"))
	    {
		if(link.equals("TOP"))
		    { result +=  link + "-" + cat;}
		else
		    { result +=  cat + "-" + link;}
	    }
	else
	    {
		result += link;
	    }

	result += ") (sem (semtype VnA)) (phon ((dir -)))))";

	boolean first=true;
	for (int j = 0; j < tokens.size(); j++) {
	    //	    System.out.println("DEBUG: parent_"+j+"=" + tokens.get(j).getNumberParent() + " parent_"+j+"_lemma=" + tokens.get(j).getRealLemma());
	    //	    System.out.println("DEBUG: children_"+i+"=" + tokens.get(i).getNumberParent() + " children_"+i+"_lemma=" + tokens.get(i).getRealLemma());

	    if(tokens.get(j).getNumberParent() == (i+1))
		{
		    // 		    System.out.println("DEBUG: parent match children\n");

		    if(first) {result += " (dependents ("; first = false;}

		    result += this.toAVM_TUT(j);
		}
	}
	if(!first) {result += "))";}
	result += ") ";

	return result;
    }


    public String  toString()
    {
	return toTUT();
    }
    public String  toTUT()
    {
	String result = "";
	//	result += ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n";
	for (int i = 0; i < tokens.size(); i++) {
	    //System.out.println(tokens.get(i).getNumber() + " "  + tokens.get(i).getLemma() + " - " +  tokens.get(i).getPos() + " - " + tokens.get(i).getNominal() + " - " +  tokens.get(i).getNominalParent() + " - " +  tokens.get(i).getNumberParent() + " - " +  tokens.get(i).getSemanticRole());
	    result += tokens.get(i) + "\n";
	}
	//result += "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n";

	return result;
    }


    public String  toAEWLIS()
    {
	String result = "<?xml version=\"1.0\"?>\n<!DOCTYPE ALEAOutput SYSTEM \"../../alea.dtd\">\n<ALEAOutput>";
	result +=  "<newSentence text=\"???\" italianText=\"???\" writtenLISSentence=\"???\" lemmaNumber=\"" + "???" + "\">\n";

	for (int i = 0; i < tokens.size(); i++) {
	    //System.out.println(tokens.get(i).getNumber() + " "  + tokens.get(i).getLemma() + " - " +  tokens.get(i).getPos() + " - " + tokens.get(i).getNominal() + " - " +  tokens.get(i).getNominalParent() + " - " +  tokens.get(i).getNumberParent() + " - " +  tokens.get(i).getSemanticRole());
	    //	    result += tokens.get(i).toTUT();
	    result += tokens.get(i).toAEWLIS();
	}
	result +="</newSentence>\n</ALEAOutput>\n";

	return result;
    }


    public String toDonna()
    {
	String result = this.toAEWLIS();

	try{
	    // Create file
	    FileWriter fstream = new FileWriter("/Users/mazzei/lavori/Projects/ATLAS/softExt/nunnariev/ATLASPlayer-ReleaseMac-110915-fnunnari/playable_files/tmp-outRealizer-00.xml");
	    BufferedWriter out = new BufferedWriter(fstream);
	    out.write(result);
	    //Close the output stream
	    out.close();
	}catch (Exception e){//Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}

	int port = 30000 ;
	String msg = "play_aewlis ./playable_files/tmp-outRealizer-00.xml";
        final DatagramSocket sock ;
        try {
            sock = new DatagramSocket();
            InetAddress addr;
            try {
                addr = InetAddress.getByName("localhost");
                byte[] buf ;
                try {
                    buf = msg.getBytes("UTF-8");
                    System.out.println("Sending '"+msg+"' to "+addr.toString()+" port "+port) ;
                    DatagramPacket pck = new DatagramPacket(buf, buf.length, addr, port) ;
                    try {
                        sock.send(pck);
                    } catch (IOException ex) {
			//                        Logger.getLogger(ProvaFrame01.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } catch (UnsupportedEncodingException ex) {
		    //                Logger.getLogger(ProvaFrame01.class.getName()).log(Level.SEVERE, null, ex);
            }
            } catch (UnknownHostException ex) {
		//                Logger.getLogger(ProvaFrame01.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (SocketException ex) {
	    //            Logger.getLogger(ProvaFrame01.class.getName()).log(Level.SEVERE, null, ex);
        }
	return result;
    }

}
