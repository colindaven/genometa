/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl.parsers.graph;

import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.Timer;

public final class BgrParser {

	/**
	 * Writes bgr format.
	 *<pre>
	 *.bgr format:
	 *  Old Header:
	 *    UTF-8 encoded seq name
	 *    UTF-8 encoded seq version
	 *    4-byte int for total number of data points
	 *  New Header:
	 *    UTF-8 encoded:
	 *       seq_name
	 *       release_name (seq version)
	 *       analysis_group_name
	 *       map_analysis_group_name
	 *       method_name
	 *       parameter_set_name
	 *       value_type_name
	 *       control_group_name
	 *    4-byte int for total number of data points
	 *  Then for each data point:
	 *    4-byte int for base position
	 *    4-byte float for value
	 * </pre>
	 */
	public static boolean writeBgrFormat(GraphSym graf, OutputStream ostr)
		throws IOException  {
		System.out.println("writing graph: " + graf);
		BufferedOutputStream bos = new BufferedOutputStream(ostr);
		DataOutputStream dos = new DataOutputStream(bos);

		Map headers = graf.getProperties();

		if (headers == null) {
			headers = new HashMap(); // use an empty map
		}		           
			if (headers.get("seq_name") == null) {
				if (graf.getGraphSeq() == null) {
					dos.writeUTF("null");
				} else {
					dos.writeUTF(graf.getGraphSeq().getID());
				}
			}
			else { dos.writeUTF((String)headers.get("seq_name")); }
			if (headers.get("release_name") == null)  {dos.writeUTF("null"); }
			else  { dos.writeUTF((String)headers.get("release_name")); }           
			if (headers.get("analysis_group_name") == null)  { dos.writeUTF("null");}
			else  { dos.writeUTF((String)headers.get("analysis_group_name")); }           
			if (headers.get("map_analysis_group_name") == null)  { dos.writeUTF("null"); }
			else  { dos.writeUTF((String)headers.get("map_analysis_group_name")); }           
			if (headers.get("method_name") == null)  { dos.writeUTF("null");}
			else  { dos.writeUTF((String)headers.get("method_name")); }          
			if (headers.get("parameter_set_name") == null)  { dos.writeUTF("null");}
			else  {dos.writeUTF((String)headers.get("parameter_set_name"));}           
			if (headers.get("value_type_name") == null)  { dos.writeUTF("null"); }
			else  { dos.writeUTF((String)headers.get("value_type_name")); }           
			if (headers.get("control_group_name") == null) { dos.writeUTF("null"); }
			else { dos.writeUTF((String)headers.get("control_group_name")); }
		writeGraphPoints(graf, dos);
		//      dos.flush();
		dos.close();
		return true;
	}

	public static GraphSym parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group, boolean ensure_unique_id)
		throws IOException  {
		Timer tim = new Timer();
		tim.start();
		int count = 0;
		BufferedInputStream bis = new BufferedInputStream(istr);
		DataInputStream dis = new DataInputStream(bis);
		HashMap<String,Object> props = new HashMap<String,Object>();
		String seq_name = dis.readUTF();
		String release_name = dis.readUTF();
		String analysis_group_name = dis.readUTF();
		System.out.println(seq_name + ", " + release_name + ", " + analysis_group_name);
		String map_analysis_group_name = dis.readUTF();
		String method_name = dis.readUTF();
		String parameter_set_name = dis.readUTF();
		String value_type_name = dis.readUTF();
		String control_group_name = dis.readUTF();
		props.put("seq_name", seq_name);
		props.put("release_name", release_name);
		props.put("analysis_group_name", analysis_group_name);
		props.put("map_analysis_group_name", map_analysis_group_name);
		props.put("method_name", method_name);
		props.put("parameter_set_name", parameter_set_name);
		props.put("value_type_name", value_type_name);
		props.put("control_group_name", control_group_name);

		int total_points = dis.readInt();
		System.out.println("loading graph from binary file, name = " + seq_name +
				", release = " + release_name +
				", total_points = " + total_points);
		int[] xcoords = new int[total_points];
		float[] ycoords = new float[total_points];
		int largest_x = 0; // assume the x-values are sorted, so the max is the last one read.
		Thread thread = Thread.currentThread();
		for (int i=0; i<total_points && ! thread.isInterrupted(); i++) {
			largest_x = xcoords[i] = dis.readInt();
			ycoords[i] = dis.readFloat();
			count++;
		}

		BioSeq seq = seq_group.getSeq(seq_name);
		if (seq == null) {
			//System.out.println("seq not found, creating new seq: '"+seq_name+"'");
			seq = seq_group.addSeq(seq_name, largest_x);
		}

		StringBuffer sb = new StringBuffer();
		append(sb, analysis_group_name);
		append(sb, value_type_name);
		append(sb, parameter_set_name);

		String graph_name;
		if (sb.length() == 0) {
			graph_name = stream_name;
		} else {
			graph_name = sb.toString();
		}

		// need to replace seq_name with name of graph (some combo of group name and conditions...)
		if (ensure_unique_id) { graph_name = AnnotatedSeqGroup.getUniqueGraphID(graph_name, seq); }
		GraphSym graf = new GraphSym(xcoords, ycoords, graph_name, seq);
		graf.setProperties(props);
		double load_time = tim.read()/1000f;
		System.out.println("loaded graf, total points = " + count);
		System.out.println("time to load graf from binary: " + load_time);
		return graf;
	}

	static void append(StringBuffer sb, String s) {
		if (s != null && ! "null".equals(s) && s.trim().length()>0) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(s);
		}
	}

	private static void writeGraphPoints(GraphSym graf, DataOutputStream dos) throws IOException {
		int total_points = graf.getPointCount();
		dos.writeInt(total_points);
		for (int i = 0; i < total_points; i++) {
			dos.writeInt(graf.getGraphXCoord(i));
			dos.writeFloat(graf.getGraphYCoord(i));
		}
	}

}
