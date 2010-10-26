package com.affymetrix.igb.view.external;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Loc {
	private static final Pattern ucscPattern = Pattern.compile("db=(\\w+)&position=(\\w+):(\\d+)-(\\d+)");
	final String db;
	final String chr;
	final int start;
	final int end;
	Loc(String db, String chromosome, int start, int end){
		this.db = db;
		this.chr = chromosome;
		this.start = start;
		this.end = end;
	}

	@Override
	public String toString(){
		return db+"\t"+chr+":"+start+"-"+end;
	}

	public int length(){
		return (end - start);
	}

	public static Loc fromUCSCQuery(String ucscQuery){
		Matcher m = ucscPattern.matcher(ucscQuery);
		if(m.matches()){
			String db = m.group(1);
			String chr = m.group(2);
			String start = m.group(3);
			String end = m.group(4);
			return new Loc(db,chr,Integer.parseInt(start),Integer.parseInt(end));
		}
		else{
			return new Loc("","",0,0);
		}
	}
}