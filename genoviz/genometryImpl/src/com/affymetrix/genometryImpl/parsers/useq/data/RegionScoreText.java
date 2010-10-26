package com.affymetrix.genometryImpl.parsers.useq.data;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

/** @author david.nix@hci.utah.edu*/
public class RegionScoreText extends RegionScore{

	//fields
	protected String text;
	private static final long serialVersionUID = 1L;

	//constructor
	public RegionScoreText (int start, int stop, float score, String text){
		super(start, stop, score);
		this.text = text;
	}

	//methods
	/**Loads a binary file containing int,int,float,String (start,stop,score,text)
	 * @return an array! or null if something bad happened.*/
	public static RegionScoreText[] oldLoadBinary_DEPRECIATED(File binaryFile, boolean sort){
		ArrayList<RegionScoreText> sss = new ArrayList<RegionScoreText>(10000);
		DataInputStream dis = null;
		int start =0;
		int stop =0;
		try {
			dis = new DataInputStream(new BufferedInputStream(new FileInputStream(binaryFile)));
			while (true){
				start = dis.readInt();
				stop = dis.readInt();
				float score = dis.readFloat();
				//read text
				byte[] barray = new byte[dis.readInt()];
				dis.readFully(barray);
				String name = new String(barray);
				sss.add(new RegionScoreText(start,stop, score, name));
			}

		} catch (EOFException eof){
			RegionScoreText[] s = new RegionScoreText[sss.size()];
			sss.toArray(s);
			if (sort) Arrays.sort(s);
			return s;
		}
		catch (Exception e){
			System.out.println("\nBad binary file "+binaryFile);
			e.printStackTrace();
			return null;
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	public String toString(){
		return start+"\t"+stop+"\t"+score+"\t"+text;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
}
