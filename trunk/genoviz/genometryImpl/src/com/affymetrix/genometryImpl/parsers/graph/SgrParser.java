package com.affymetrix.genometryImpl.parsers.graph;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;

public final class SgrParser {
	private static final boolean DEBUG = false;
	private static final Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace

	public static List<GraphSym> parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
					boolean annotate_seq)
					throws IOException {
		return parse(istr, stream_name, seq_group, annotate_seq, true);
	}

	public static List<GraphSym> parse(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group,
					boolean annotate_seq, boolean ensure_unique_id)
					throws IOException {
		if (DEBUG) {
			System.out.println("Parsing with SgrParser: " + stream_name);
		}
		List<GraphSym> results = new ArrayList<GraphSym>();

		try {
			InputStreamReader isr = new InputStreamReader(istr);
			BufferedReader br = new BufferedReader(isr);

			Map<String, IntArrayList> xhash = new HashMap<String, IntArrayList>();
			Map<String, FloatArrayList> yhash = new HashMap<String, FloatArrayList>();

			String gid = stream_name;
			if (ensure_unique_id) {
				// Making sure the ID is unique on the whole genome, not just this seq
				// will make sure the GraphState is also unique on the whole genome.
				gid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq_group);
			}
			
			parseLines(br, xhash, yhash);

			createResults(xhash, seq_group, yhash, gid, results);

		} catch (Exception e) {
			e.printStackTrace();
			if (!(e instanceof IOException)) {
				IOException ioe = new IOException("Trouble reading SGR file: " + stream_name);
				ioe.initCause(e);
				throw ioe;
			}
		}

		return results;
	}

	private static void parseLines(BufferedReader br, Map<String, IntArrayList> xhash, Map<String, FloatArrayList> yhash) throws IOException, NumberFormatException {
		String line;
		while ((line = br.readLine()) != null) {
			if (line.charAt(0) == '#') {
				continue;
			}
			if (line.charAt(0) == '%') {
				continue;
			}
			String[] fields = line_regex.split(line);
			String seqid = fields[0];
			IntArrayList xlist = xhash.get(seqid);
			if (xlist == null) {
				xlist = new IntArrayList();
				xhash.put(seqid, xlist);
			}
			FloatArrayList ylist = yhash.get(seqid);
			if (ylist == null) {
				ylist = new FloatArrayList();
				yhash.put(seqid, ylist);
			}
			int x = Integer.parseInt(fields[1]);
			float y = Float.parseFloat(fields[2]);
			if (DEBUG) {
				System.out.println("seq = " + seqid + ", x = " + x + ", y = " + y);
			}
			xlist.add(x);
			ylist.add(y);
		}
	}

	public static boolean writeSgrFormat(GraphSym graf, OutputStream ostr) throws IOException {
		BioSeq seq = graf.getGraphSeq();
		if (seq == null) {
			throw new IOException("You cannot use the '.sgr' format when the sequence is unknown. Use '.gr' instead.");
		}
		String seq_id = seq.getID();

		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			bos = new BufferedOutputStream(ostr);
			dos = new DataOutputStream(bos);
			writeGraphPoints(graf, dos, seq_id);
		} finally {
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(dos);
		}
		return true;
	}

	private static void writeGraphPoints(GraphSym graf, DataOutputStream dos, String seq_id) throws IOException {
		int total_points = graf.getPointCount();
		for (int i = 0; i < total_points; i++) {
			dos.writeBytes(seq_id + "\t" + graf.getGraphXCoord(i) + "\t"
					+ graf.getGraphYCoordString(i) + "\n");
		}
	}


	private static void createResults(
			Map<String, IntArrayList> xhash, AnnotatedSeqGroup seq_group, Map<String, FloatArrayList> yhash, String gid, List<GraphSym> results) {
		for (Map.Entry<String, IntArrayList> keyval : xhash.entrySet()) {
			String seqid = keyval.getKey();
			BioSeq aseq = seq_group.getSeq(seqid);
			IntArrayList xlist = keyval.getValue();
			FloatArrayList ylist = yhash.get(seqid);
			if (aseq == null) {
				aseq = seq_group.addSeq(seqid, xlist.get(xlist.size() - 1));
			}
			int[] xcoords = Arrays.copyOf(xlist.elements(), xlist.size());
			xlist = null;
			float[] ycoords = Arrays.copyOf(ylist.elements(), ylist.size());
			ylist = null;

			//Is data sorted?
			int xcount = xcoords.length;
			boolean sorted = true;
			int prevx = Integer.MIN_VALUE;
			for (int i = 0; i < xcount; i++) {
				int x = xcoords[i];
				if (x < prevx) {
					sorted = false;
					break;
				}
				prevx = x;
			}
			
			if (!sorted) {
				GrParser.sortXYDataOnX(xcoords, ycoords);
			}

			GraphSym graf = new GraphSym(xcoords, ycoords, gid, aseq);
			results.add(graf);
		}
	}

}
