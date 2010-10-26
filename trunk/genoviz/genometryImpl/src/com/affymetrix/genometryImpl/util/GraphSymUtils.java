package com.affymetrix.genometryImpl.util;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import java.io.*;
import java.util.*;
import java.net.URL;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.CompositeGraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.parsers.graph.SgrParser;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.parsers.graph.GrParser;
import com.affymetrix.genometryImpl.parsers.graph.BgrParser;
import com.affymetrix.genometryImpl.parsers.graph.WiggleParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqGraphParser;
import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;

public final class GraphSymUtils {

	/** Pref for whether newly-constructed graph glyphs should only show a
	 *  limited range of values.
	 */
	public static final String PREF_APPLY_PERCENTAGE_FILTER = "apply graph percentage filter";
	public static final String PREF_USE_URL_AS_NAME = "Use complete URL as graph name";
	public static final boolean default_use_url_as_name = false;

	private static final int MAX_INITCAP = 1024*1024;

	/**
	 *  Transforms a restricted type of GraphSym based on a SeqSymmetry.
	 *  This is _not_ a general algorithm for transforming GraphSyms with an arbitrary mapping sym --
	 *    it is simpler, and assumes that the mapping symmetry is of depth=2 (or possibly 1?) and
	 *    breadth = 2, and that they're "regular" (parent sym and each child sym have seqspans pointing
	 *    to same two BioSeqs.
	 *  It should work fine on GraphIntervalSym objects as well as regular GraphSym objects.
	 *  ensure_unique_id indicates whether should try to muck with id so it's not same as any GraphSym on the seq
	 *     (including the original_graf, if it's one of seq's annotations)
	 *     For transformed GraphSyms probably should set ensure_unique_id to false, unless result is actually added onto toseq...
	 */
	public static GraphSym transformGraphSym(GraphSym original_graf, SeqSymmetry mapsym) {
		if (original_graf.getPointCount() == 0) {
			return null;
		}
		BioSeq fromseq = original_graf.getGraphSeq();
		SeqSpan fromspan = mapsym.getSpan(fromseq);

		if (fromseq == null || fromspan == null) {
			return null;
		}
		GraphSym new_graf = null;
		BioSeq toseq = SeqUtils.getOtherSeq(mapsym, fromseq);

		SeqSpan tospan = mapsym.getSpan(toseq);
		if (toseq == null || tospan == null) {
			return null;
		}
		double graf_base_length = original_graf.getMaxXCoord() - original_graf.getMinXCoord();

		// calculating graf length from xcoords, since graf's span
		//    is (usually) incorrectly set to start = 0, end = seq.getLength();
		double points_per_base = (double) original_graf.getPointCount() / graf_base_length;
		int initcap = (int) (points_per_base * toseq.getLength() * 1.5);
		if (initcap > MAX_INITCAP) {
			initcap = MAX_INITCAP;
		}
		IntArrayList new_xcoords = new IntArrayList(initcap);
		FloatArrayList new_ycoords = new FloatArrayList(initcap);
		IntArrayList new_wcoords = null;
		if (hasWidth(original_graf)) {
			new_wcoords = new IntArrayList(initcap);
		}
		
		addCoords(mapsym, fromseq, toseq, original_graf, new_xcoords, new_ycoords, new_wcoords);

		return createGraphSym(new_xcoords, new_ycoords, original_graf, toseq, new_wcoords, new_graf);
	}

	private static void addCoords(
			SeqSymmetry mapsym, BioSeq fromseq, BioSeq toseq, GraphSym original_graf,
			IntArrayList new_xcoords, FloatArrayList new_ycoords, IntArrayList new_wcoords) {
		List<SeqSymmetry> leaf_syms = SeqUtils.getLeafSyms(mapsym);
		for (SeqSymmetry leafsym : leaf_syms) {
			SeqSpan fspan = leafsym.getSpan(fromseq);
			SeqSpan tspan = leafsym.getSpan(toseq);
			if (fspan == null || tspan == null) {
				continue;
			}
			boolean opposite_spans = fspan.isForward() ^ tspan.isForward();
			int ostart = fspan.getStart();
			int oend = fspan.getEnd();
			double scale = tspan.getLengthDouble() / fspan.getLengthDouble();
			if (opposite_spans) {
				scale = -scale;
			}
			double offset = tspan.getStartDouble() - (scale * fspan.getStartDouble());
			int kmax = original_graf.getPointCount();
			int start_index = 0;
			if (!hasWidth(original_graf)) {
				// If there are no width coordinates, then we can speed up the
				// drawing by determining the start_index of the first x-value in range.
				// If there are widths, this is much harder to determine, since
				// even something starting way over to the left but having a huge width
				// could intersect our region.  So when there are wcoords, don't
				// try to determine start_index.  Luckily, when there are widths, there
				// tend to be fewer graph points to deal with.
				// assumes graph is sorted
				start_index = original_graf.determineBegIndex(ostart - 1);
			}
			for (int k = start_index; k < kmax; k++) {
				final int old_xcoord = original_graf.getGraphXCoord(k);
				if (old_xcoord >= oend) {
					break; // since the array is sorted, we can stop here
				}
				int new_xcoord = (int) ((scale * old_xcoord) + offset);
				// new_x2coord will represent x + width: initial assumption is width is zero
				int new_x2coord = new_xcoord;
				if (hasWidth(original_graf)) {
					final int old_x2coord = old_xcoord + ((GraphIntervalSym) original_graf).getGraphWidthCoord(k);
					new_x2coord = (int) ((scale * old_x2coord) + offset);
					if (new_x2coord >= tspan.getEnd()) {
						new_x2coord = tspan.getEnd();
					}
				}
				final int tstart = tspan.getStart();
				if (new_xcoord < tstart) {
					if (!hasWidth(original_graf)) {
						continue;
					} else if (new_x2coord > tstart) {
						new_xcoord = tstart;
					} else {
						continue;
					}
				}
				new_xcoords.add(new_xcoord);
				new_ycoords.add(original_graf.getGraphYCoord(k));
				if (hasWidth(original_graf)) {
					int new_wcoord = new_x2coord - new_xcoord;
					new_wcoords.add(new_wcoord);
				}
			}
		}
	}


	private static GraphSym createGraphSym(
			IntArrayList new_xcoords, FloatArrayList new_ycoords, GraphSym original_graf, BioSeq toseq,
			IntArrayList new_wcoords, GraphSym new_graf) {
		String newid = original_graf.getID();
		// create GraphSym.
		new_xcoords.trimToSize();
		int[] new_xcoordArr = new_xcoords.elements();
		new_ycoords.trimToSize();
		float[] new_ycoordArr = new_ycoords.elements();
		if (!hasWidth(original_graf)) {
			new_graf = new GraphSym(new_xcoordArr, new_ycoordArr, newid, toseq);
		} else {
			new_wcoords.trimToSize();
			int[] new_wcoordArr = new_wcoords.elements();
			new_graf = new GraphIntervalSym(new_xcoordArr, new_wcoordArr, new_ycoordArr, newid, toseq);
		}
		new_graf.setGraphName(original_graf.getGraphName());
		return new_graf;
	}


	/** Detects whether the given filename ends with a recognized ending for
	 *  a graph filetype. Compression endings like gz and zip are removed
	 *  before testing the name.
	 */
	public static boolean isAGraphFilename(String name) {
		String lc = GeneralUtils.stripEndings(name).toLowerCase();
		return (
				lc.endsWith(".gr") ||
				lc.endsWith(".bgr") ||
				lc.endsWith(".bar") ||
				lc.endsWith(".sgr") ||
				lc.endsWith(".wig")
			   );
	}

	/**
	 *  Reads one or more graphs from an input stream.
	 *  Some graph file formats can contain only one graph, others contain
	 *  more than one.  For consistency, always returns a List (possibly empty).
	 *  Will accept "bar", "bgr", "gr", or "sgr".
	 *  Loaded graphs will be attached to their respective BioSeq's, if they
	 *  are instances of BioSeq.
	 *  @param seq  Ignored in most cases.  But for "gr" files that
	 *   do not specify a BioSeq, use this parameter to specify it.  If null
	 *   then GenometryModel.getSelectedSeq() will be used.
	 */
	public static List<GraphSym> readGraphs(InputStream istr, String stream_name, AnnotatedSeqGroup seq_group, BioSeq seq)
			throws IOException  {
		List<GraphSym> grafs = null;
		StringBuffer stripped_name = new StringBuffer();
		InputStream newstr = GeneralUtils.unzipStream(istr, stream_name, stripped_name);
		String sname = stripped_name.toString().toLowerCase();

		if (sname.endsWith(".bar"))  {
			grafs = BarParser.parse(newstr, GenometryModel.getGenometryModel(), seq_group, null, 0, Integer.MAX_VALUE, stream_name, true);
		}
		else if (sname.endsWith(".useq")) {
			USeqGraphParser up = new USeqGraphParser();
			grafs = up.parseGraphSyms(istr, GenometryModel.getGenometryModel(), stream_name, null);
		}
		else if (sname.endsWith(".gr")) {
			if (seq == null) {
				seq = GenometryModel.getGenometryModel().getSelectedSeq();
			}
			// If this is a newly-created seq group, then go ahead and add a new 
			// unnamed seq to it if necessary.
			if (seq_group.getSeqCount() == 0) {
				seq = seq_group.addSeq("unnamed", 1000);
			}
			if (seq == null) {
				throw new IOException("Must select a sequence before loading a graph of type 'gr'");
			}
			GraphSym graph = GrParser.parse(newstr, seq, stream_name);
			int max_x = graph.getMaxXCoord();
			BioSeq gseq = graph.getGraphSeq();
			seq_group.addSeq(gseq.getID(), max_x); // this stretches the seq to hold the graph
			grafs = wrapInList(graph);
		}
		else if (sname.endsWith(".bgr")) {
			grafs = wrapInList(BgrParser.parse(newstr, stream_name, seq_group, true));
		}
		else if (sname.endsWith(".sgr")) {
			grafs = SgrParser.parse(newstr, stream_name, seq_group, false);
		}
		else if (sname.endsWith(".wig")) {
			WiggleParser wig_parser = new WiggleParser();
			grafs = wig_parser.parse(newstr, seq_group, false, stream_name);
		} else {
			throw new IOException("Unrecognized filename for a graph file:\n"+stream_name);
		}

		if (grafs == null) {
			grafs = Collections.<GraphSym>emptyList();
		}

		return grafs;
	}
	
	/**
	 * Calls {@link AnnotatedSeqGroup#getUniqueGraphID(String,BioSeq)}.
	 */
	public static String getUniqueGraphID(String id, BioSeq seq) {
		return AnnotatedSeqGroup.getUniqueGraphID(id, seq);
	}

	private static List<GraphSym> wrapInList(GraphSym gsym) {
		List<GraphSym> grafs = null;
		if (gsym != null) {
			grafs = new ArrayList<GraphSym>();
			grafs.add(gsym);
		}
		return grafs;
	}

	/*
	 *  Does some post-load processing of Graph Syms.
	 *  For each GraphSym in the list,
	 *  Adds it as an annotation of the BioSeq it refers to.
	 *  Sets the "source_url" to the given stream name.
	 *  Set the GenericFeature in its style.
	 *  Calls setGraphName() with the given name;
	 *  Converts to a trans frag graph if "TransFrag" is part of the graph name.
	 *  @param grafs  a List, empty or null is OK.
	 */
	//@Deprecated
	public static void processGraphSyms(List<GraphSym> grafs, String original_stream_name, GenericFeature feature) {
		if (grafs == null) {
			return;
		}
		for (GraphSym gsym : grafs) {
			BioSeq gseq = gsym.getGraphSeq();
			if (gseq != null) {
				String gid = gsym.getID();
				String newid = getUniqueGraphID(gid, gseq);
				//TODO: Instead of re-setting the graph ID, a unique ID should have been used in the constructor
				if (!(newid.equals(gid))) {
					gsym.setID(newid);
				}
			}
			gsym.lockID();
			if (gseq != null) {
				gseq.addAnnotation(gsym);
			}

			gsym.setProperty("source_url", original_stream_name);

			gsym.getGraphState().getTierStyle().setFeature(feature);

			if ((gsym.getGraphName() != null) && (gsym.getGraphName().indexOf("TransFrag") >= 0)) {
				gsym = GraphSymUtils.convertTransFragGraph(gsym);
			}
		}
	}


	/** Writes out in a variety of possible formats depending
	 *  on the suffix of the filename.
	 *  Formats include ".gr", ".sgr", ".sin" == ".egr", ".bgr".
	 *  @param seq_group the AnnotatedSeqGroup the graph is on, needed for ".wig", ".egr", and ".sin" formats.
	 */
	public static void writeGraphFile(GraphSym gsym, AnnotatedSeqGroup seq_group, String file_name) throws IOException {
		BufferedOutputStream bos = null;
		try {
			if (file_name.endsWith(".bgr")) {
				bos = new BufferedOutputStream(new FileOutputStream(file_name));
				BgrParser.writeBgrFormat(gsym, bos);
			} else if (file_name.endsWith(".gr")) {
				bos = new BufferedOutputStream(new FileOutputStream(file_name));
				GrParser.writeGrFormat(gsym, bos);
			} else if (file_name.endsWith(".sgr")) {
				bos = new BufferedOutputStream(new FileOutputStream(file_name));
				SgrParser.writeSgrFormat(gsym, bos);
			} else if (file_name.endsWith(".egr") || file_name.endsWith(".sin")) {
				if (gsym instanceof GraphIntervalSym) {
					String genome_name = null;
					if (seq_group != null) {
						genome_name = seq_group.getID();
					}
					bos = new BufferedOutputStream(new FileOutputStream(file_name));
					ScoredIntervalParser.writeEgrFormat((GraphIntervalSym) gsym, genome_name, bos);
				} else {
					throw new IOException("Not the correct graph type for the '.egr' format.");
				}
			} else if (file_name.endsWith(".wig")) {
				if (gsym instanceof GraphIntervalSym) {
					GraphIntervalSym gisym = (GraphIntervalSym) gsym;
					String genome_name = null;
					if (seq_group != null) {
						genome_name = seq_group.getID();
					}
					bos = new BufferedOutputStream(new FileOutputStream(file_name));
					WiggleParser.writeBedFormat(gisym, genome_name, bos);
				} else {
					throw new IOException("Not the correct graph type for the '.wig' format.");
				}
			} else {
				throw new IOException("Graph file name does not have the correct extension");
			}
		} finally {
			GeneralUtils.safeClose(bos);
		}
	}

	/**
	 *  Calculate percentile rankings of graph values.
	 *  In the resulting array, the value of scores[i] represents
	 *  the value at percentile (100 * i)/(scores.length - 1).
	 *
	 *  This is an expensive calc, due to sort of copy of scores array
	 *    Plan to change this to a sampling strategy if scores.length greater than some cutoff (maybe 100,000 ?)
	 */
	public static float[] calcPercents2Scores(float[] scores, float bins_per_percent) {
		int max_sample_size = 100000;
		float abs_max_percent = 100.0f;
		float percents_per_bin = 1.0f / bins_per_percent;

		int num_scores = scores.length;
		float[] ordered_scores;
		// sorting a large array is an expensive operation timewise, so if scores array is
		//   larger than a certain size, do approximate ranking instead by sampling the scores array
		//   and ranking over smaple
		//
		// in performance comparisons of System.arraycopy() vs. piecewise loop,
		//     piecewise takes about twice as long for copying same number of elements,
		//     but this 2x performance hit should be overwhelmed by time taken for larger array sort
		if (num_scores > (2 * max_sample_size) ) {
			int sample_step = num_scores / max_sample_size;
			int sample_index = 0;
			ordered_scores = new float[max_sample_size];
			for (int i=0; i<max_sample_size; i++) {
				ordered_scores[i] = scores[sample_index];
				sample_index += sample_step;
			}
		}
		else {
			ordered_scores = new float[num_scores];
			System.arraycopy(scores, 0, ordered_scores, 0, num_scores);
		}
		Arrays.sort(ordered_scores);
		int num_percents = (int)(abs_max_percent * bins_per_percent + 1);
		float[] percent2score = new float[num_percents];

		float scores_per_percent = ordered_scores.length / 100.0f;
		for (float percent = 0.0f; percent <= abs_max_percent; percent += percents_per_bin) {
			int score_index = (int)(percent * scores_per_percent);
			if (score_index >= ordered_scores.length) { score_index = ordered_scores.length -1; }
			percent2score[Math.round(percent * bins_per_percent)] = ordered_scores[score_index];
		}
		// just making sure max 100% is really 100%...
		percent2score[percent2score.length - 1] = ordered_scores[ordered_scores.length - 1];
		return percent2score;
	}


	private static GraphSym convertTransFragGraph(GraphSym trans_frag_graph) {
		int xcount = trans_frag_graph.getPointCount();
		if (xcount < 2) { return null; }

		int transfrag_max_spacer = 20;
		BioSeq seq = trans_frag_graph.getGraphSeq();
		IntArrayList newx = new IntArrayList();
		FloatArrayList newy = new FloatArrayList();

		// transfrag ycoords should be irrelevant
		int xmin = trans_frag_graph.getMinXCoord();
		float y_at_xmin = trans_frag_graph.getGraphYCoord(0);
		int prevx = xmin;
		float prevy = y_at_xmin;
		int curx = xmin;
		float cury = y_at_xmin;
		for (int i=1; i<xcount; i++) {
			curx = trans_frag_graph.getGraphXCoord(i);
			cury = trans_frag_graph.getGraphYCoord(i);
			if ((curx - prevx) > transfrag_max_spacer) {
				newx.add(xmin);
				newy.add(y_at_xmin);
				newx.add(prevx);
				newy.add(prevy);
				if (i == (xcount - 2)) {
					System.out.println("breaking, i = " + i + ", xcount = " + xcount);
					break;
				}
				xmin = curx;
				y_at_xmin = cury;
				i++;
			}
			prevx = curx;
			prevy = cury;
		}
		newx.add(xmin);
		newy.add(y_at_xmin);
		newx.add(curx);
		newy.add(cury);
		String newid = GraphSymUtils.getUniqueGraphID(trans_frag_graph.getGraphName(), seq);
		newx.trimToSize();
		newy.trimToSize();
		GraphSym span_graph = new GraphSym(newx.elements(), newy.elements(), newid, seq);

		// copy properties over...
		span_graph.setProperties(trans_frag_graph.cloneProperties());

		// add transfrag property...
		span_graph.setProperty("TransFrag", "TransFrag");
		return span_graph;

	}

	 /**
     *  Given a child GraphSym, find the appropriate parent [Composite]GraphSym and add child to it
     *
     *  Assumes ids of parent graphs are unique among annotations on seq
     *  Also use Das2FeatureRequestSym overlap span as span for child GraphSym
     *  Uses unique graph ID (generally stream URI plus track info), type name as graph name
     */
   public static void addChildGraph(GraphSym cgraf, String id, String name, SeqSpan overlapSpan) {
		BioSeq aseq = cgraf.getGraphSeq();
		GraphSym pgraf = getParentGraph(id, name, aseq, cgraf);

		// since GraphSyms get a span automatically set to the whole seq when constructed, need to first
		//    remove that span, then add overlap span from FeatureRequestSym
		//    could instead create new span based on start and end xcoord, but for better integration with
		//    rest of Das2ClientOptimizer span of request is preferred
		cgraf.removeSpan(cgraf.getSpan(aseq));
		cgraf.addSpan(overlapSpan);
		pgraf.addChild(cgraf);
		//add properties of child to parent
		pgraf.setProperties(cgraf.getProperties());
	}


	private static GraphSym getParentGraph(String id, String name, BioSeq aseq, GraphSym cgraf) {
		//is it a useq graph? modify name and id for strandedness? must uniquify with strand info since no concept of stranded data from same graph file
		if (id.endsWith(USeqUtilities.USEQ_EXTENSION_WITH_PERIOD) || name.endsWith(USeqUtilities.USEQ_EXTENSION_WITH_PERIOD)){
			Object obj = cgraf.getProperty(GraphSym.PROP_GRAPH_STRAND);
			if (obj != null){
				String strand = null;
				Integer strInt = (Integer)obj;
				if (strInt.equals(GraphSym.GRAPH_STRAND_PLUS)) strand = "+";
				else if (strInt.equals(GraphSym.GRAPH_STRAND_MINUS)) strand = "-";
				if (strand != null){
					id = id+strand;
					name = name+strand;
				}
			}
		}

		GraphSym pgraf = (GraphSym) aseq.getAnnotation(id);
		if (pgraf == null) {
			// don't need to uniquify ID, since already know it's null (since no sym retrieved from aseq)
			pgraf = new CompositeGraphSym(id, aseq);
			pgraf.setGraphName(name);
			aseq.addAnnotation(pgraf);
		}
		return pgraf;
	}


	private static boolean hasWidth(GraphSym graf) {
		return graf instanceof GraphIntervalSym;
	}

	/**
	 *  Return a graph name for the given URL.  The graph name is typically just
	 *  the last portion of the URL, but the entire URL may be used, depending on
	 *  the preference GraphGlyphUtils.PREF_USE_URL_AS_NAME.
	 */
	public static String getGraphNameForURL(URL furl) {
		String name;
		boolean use_full_url = PreferenceUtils.getGraphPrefsNode().getBoolean(
				PREF_USE_URL_AS_NAME, default_use_url_as_name);
		if (use_full_url) {
			name = furl.toExternalForm();
		} else { // use only the filename, not the whole url
			name = furl.getFile();
			int index = name.lastIndexOf('/');
			if (index > 0) {
				String last_name = name.substring(index + 1);
				if (last_name.length() > 0) {
					name = GeneralUtils.URLDecode(last_name);
				}
			}
		}
		return name;
	}

	public static String getGraphNameForFile(String name) {
		boolean use_full_url = PreferenceUtils.getGraphPrefsNode().getBoolean(
				PREF_USE_URL_AS_NAME, default_use_url_as_name);
		if (use_full_url) {
			// leave the name alone
		} else { // use only the filename, not the whole url
			int index = name.lastIndexOf(System.getProperty("file.separator"));
			if (index > 0) {
				String last_name = name.substring(index + 1);
				if (last_name.length() > 0) {
					// shouldn't need to do URLDecoder.decode()
					name = last_name;
				}
			}
		}
		return name;
	}
}
