package realizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class tokenAWLIS {
    private String word;
    private String lemma;
    private String realLemma;
    private String atlasId;
    private String handsNumber;
    private String pos;
    private String position_x;
    private String position_y;
    private String position_z;
    private Boolean plural;
    private String nominal;
    private int number;
    private String nominalParent;
    private int numberParent;
    private String semanticRole;

    public tokenAWLIS() {
    }

    public tokenAWLIS(String w, String l, String p, String px, String py, String pz, Boolean pl, String nom, int num, String nop, int nup,String sr) {
	setWord(w);
	setLemma(l);
	setRealLemma(l);
	setPos(p);
	setPositions(px,py,pz);
	setPlural(pl);
	setNominal(nom);
	setNumber(num);
	setNominalParent(nop);
	setNumberParent(nup);
	setSemanticRole(sr);
    }


    private void setWord(String w)            {	word = w; }
    private void setLemma(String l)           {	lemma = l; }
    private void setPos(String p)             {	pos = p; }
    private void setPlural(Boolean p)         {	plural = p; }
    private void setNominal(String nom)       {	nominal = nom; }
    public  void setNumber(int num)           {	number  = num; }
    private void setNominalParent(String nop) {	nominalParent = nop; }
    public  void setNumberParent(int nup)     {	numberParent = nup; }
    private void setSemanticRole(String sr)   {	semanticRole = sr; }
    private void setPositions(String px, String py, String pz) {position_x=px; position_y=py; position_z=pz;}
    private void setRealLemma(String l) {
	String p = "([^\\-]+)-([^\\-]+)-([1,2])(\\_.+)?";
	Pattern pattern = Pattern.compile(p);
	Matcher matcher = pattern.matcher(l);
	matcher.find();

	try {
	    if(matcher.groupCount() >= 1)
		{ realLemma = matcher.group(1);}
	    else
		{realLemma="VnA";}

	    if(matcher.groupCount() >= 2)
		{ atlasId  = matcher.group(2);}
	    else
		{ atlasId = "VnA"; }

	    if(matcher.groupCount() >= 3)
		{ handsNumber = matcher.group(3);}
	    else
		{ handsNumber = "VnA"; }
	}  catch (Exception e) {
	    System.out.println("EXCEPTION: Problema sul lemma >" + l + "<");
	    realLemma = lemma;
	    atlasId = "VnA";
	    handsNumber = "VnA";
	}
    }


    public void resetParent(int nup, String nop, String sr) {numberParent = nup; nominalParent = nop; semanticRole = sr;}


    public String  getWord()          {return word;}
    public String  getLemma()         {return lemma;}
    public String  getRealLemma()     {return realLemma;}
    public String  getAtlasId()       {return atlasId;}
    public String  getHandsNumber()   {return handsNumber;}
    public String  getPos()           {return pos;}
    public Boolean getPlural()        {return plural;}
    public String  getNominal()       {return nominal;}
    public int     getNumber()        {return number;}
    public String  getNominalParent() {return nominalParent;}
    public int     getNumberParent()  {return numberParent;}
    public String  getSemanticRole()  {return semanticRole;}
    public String  getPositionX()     {return position_x;}
    public String  getPositionY()     {return position_y;}
    public String  getPositionZ()     {return position_z;}

    public String  toString()  {
	return toTUT();
    }



    public String  toTUT()  {
	String result =  getNumber() + " " + getLemma() + " (";

	if(!getPos().matches("^VnA$") )
	    result += " " + getPos();

	if(!getPlural())
	    result += " SG";
	else
	    result += " PL";

	if(!getPositionX().matches("^VnA$") )
	    result += " " + getPositionX();
	if(!getPositionY().matches("^VnA$") )
	    result += " " + getPositionY();
	if(!getPositionZ().matches("^VnA$") )
	    result += " " + getPositionZ();

	result += ")";


	//result += "{@" + getNominal() + "} ";
	result += " [" + getSemanticRole() + " - " +  getNumberParent() + "]";
	//result += " {@"+getNominal() + "<" + getSemanticRole()  +  ">@"+ getNominalParent() + "}";//out the nominals structure
	return result;
    }


    public String  toAEWLIS()  {

	/*String p = "([^\\-]+)-([^\\-]+)-([1,2])(\\_.+)?";
	Pattern pattern = Pattern.compile(p);
	Matcher matcher = pattern.matcher(getLemma());
	matcher.find();

	String lemma=matcher.group(1);
	String code=matcher.group(2);
	String hands=matcher.group(3);*/

	String result = "<newLemma lemma=\"";
	result += realLemma + "\" idAtlasSign=\"" + atlasId +"\">\n";
	result +=  "<handsNumber>"+ handsNumber +"</handsNumber>\n";


	String px ="0.0"; String py="0.0"; String pz="0.0";

	if(getPositionX().matches("R1") )
	    px = "-0.6";
	if(getPositionX().matches("R2") )
	    px = "-0.4";
	if(getPositionX().matches("R3") )
	    px = "-0.2";
	if(getPositionX().matches("N") )
	    px = "-0.0";
	if(getPositionX().matches("L1") )
	    px = "0.6";
	if(getPositionX().matches("L2") )
	    px = "0.4";
	if(getPositionX().matches("L3") )
	    px = "0.2";


	result += "<signSpatialLocation>" + px +" "+ py + " " + pz +" </signSpatialLocation>\n";
	result += "</newLemma>\n";


	return result;
    }


}
