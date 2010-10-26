package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.util.PropertyViewHelper;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.TierLabelManager;
import java.util.*;
import java.text.NumberFormat;
import javax.swing.JPanel;
import javax.swing.table.*;
import com.affymetrix.igb.util.JTableCutPasteAdapter;
import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;

public final class PropertyView extends JPanel implements SymSelectionListener {

	// the table showing name-value pairs
	private static final JTable table = new JTable();
	private final JScrollPane scroll_pane = new JScrollPane();
	private TableRowSorter<TableModel> sorter;
	public static final String PROPERTY = "property";
	public static final String DEFAULT_TITLE = "Property Sheet";
	private static final List<String> prop_order = determineOrder();
	private static final PropertyViewHelper helper = new PropertyViewHelper(table);
	Set<PropertyListener> propertyListeners = new HashSet<PropertyListener>();

	public PropertyView() {
		super();
		determineOrder();
		JViewport jvp = new JViewport();
		scroll_pane.setColumnHeaderView(jvp);
		new JTableCutPasteAdapter(table, true);
		this.setPreferredSize(new java.awt.Dimension(100, 250));
		this.setMinimumSize(new java.awt.Dimension(100, 250));
		GenometryModel.getGenometryModel().addSymSelectionListener(this);
		propertyListeners.add(Application.getSingleton().getMapView().getMouseListener());
	}


	// The general order these fields should show up in.
	private static List<String> determineOrder() {
		List<String> orderList = new ArrayList<String>(20);
		orderList.add("gene name");
		orderList.add("name");
		orderList.add("id");
		orderList.add("chromosome");
		orderList.add("start");
		orderList.add("end");
		orderList.add("length");
		orderList.add("min score");
		orderList.add("max score");
		orderList.add("type");
		orderList.add("same orientation");
		orderList.add("query length");
		orderList.add("# matches");
		orderList.add("# target inserts");
		orderList.add("# target bases inserted");
		orderList.add("# query bases inserted");
		orderList.add("# query inserts");
		orderList.add("seq id");
		orderList.add("cds min");
		orderList.add("cds max");
		orderList.add("description");
		orderList.add("loadmode");
		orderList.add("feature url");

		return orderList;
	}

	public void symSelectionChanged(SymSelectionEvent evt) {
		Object src = evt.getSource();
		// if selection event originally came from here, then ignore it...
		if (src == this) {
			return;
		}
		SeqMapView mapView = null;
		TierLabelManager tlm = null;

		if (src instanceof SeqMapView) {
			mapView = (SeqMapView) src;
			tlm = mapView.getTierManager();
		} else if (src instanceof TierLabelManager){
			tlm = (TierLabelManager)src;
		}
		
		showSyms(evt.getSelectedSyms(), mapView, tlm);
	}

	@SuppressWarnings("unchecked")
	private void showSyms(List<SeqSymmetry> selected_syms, SeqMapView seqMap, TierLabelManager tlm) {
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		for (SeqSymmetry sym : selected_syms) {
			Map<String, Object> props = determineProps(sym, seqMap);
			if (props != null) {
				propList.add(props);
			}
			
		}

		if(seqMap != null)
			addGlyphInfo(propList, seqMap.getSeqMap().getSelected(), selected_syms);

		if(tlm != null)
			addTierInfo(propList, tlm);
		
		Map<String, Object>[] prop_array = propList.toArray(new Map[propList.size()]);

		this.showProperties(prop_array, prop_order, "");
	}

	public static void addTierInfo(List<Map<String, Object>> propList, TierLabelManager handler){
		List<Map<String, Object>> tierProp = handler.getTierProperties();

		if(!tierProp.isEmpty()){
			propList.addAll(tierProp);
		}
	}


	@SuppressWarnings("unchecked")
	private static void addGlyphInfo(List<Map<String, Object>> propList, List<GlyphI> glyphs, List<SeqSymmetry> selected_syms){
		for (GlyphI glyph : glyphs) {
			
			if (glyph.getInfo() instanceof SeqSymmetry
					&& selected_syms.contains((SeqSymmetry) glyph.getInfo())) {
				continue;
			}

			Map<String, Object> props = null;
			if(glyph.getInfo() instanceof Map){
				 props = (Map<String, Object>) glyph.getInfo();
			} else {
				props = new HashMap<String, Object>();
			}
			
			boolean direction = true;
			if(props.containsKey("direction")){
				if(((String)props.get("direction")).equals("reverse"))
					direction = false;
			}

			Rectangle2D.Double boundary = glyph.getSelectedRegion();
			int start = (int) boundary.getX();
			int length = (int) boundary.getWidth();
			int end = start + length;
			if(!direction){
				int temp = start;
				start = end;
				end = temp;
			}
			props.put("start", start);
			props.put("end", end);
			props.put("length", length);

			propList.add(props);
		}
	}
	
	private static Map<String, Object> determineProps(SeqSymmetry sym, SeqMapView seqMap) {
		Map<String, Object> props = null;
		if (sym instanceof SymWithProps) {
			// using Propertied.cloneProperties() here instead of Propertied.getProperties()
			//   because adding start, end, id, and length as additional key-val pairs to props Map
			//   and don't want these to bloat up sym's properties
			props = ((SymWithProps) sym).cloneProperties();
		}
		if (props == null && sym instanceof DerivedSeqSymmetry) {
			SeqSymmetry original_sym = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
			if (original_sym instanceof SymWithProps) {
				props = ((SymWithProps) original_sym).cloneProperties();
			}
		}
		if (props == null) {
			// make an empty hashtable if sym has no properties...
			props = new Hashtable<String, Object>();
		}
		String symid = sym.getID();
		if (symid != null) {
			props.put("id", symid);
		}
		if (seqMap != null) {
			SeqSpan span = seqMap.getViewSeqSpan(sym);
			if (span != null) {
				String chromID = span.getBioSeq().getID();
				props.put("chromosome", chromID);
				props.put("start",
						NumberFormat.getIntegerInstance().format(span.getStart()));
				props.put("end",
						NumberFormat.getIntegerInstance().format(span.getEnd()));
				props.put("length",
						NumberFormat.getIntegerInstance().format(span.getLength()));
				props.remove("seq id"); // this is redundant if "chromosome" property is set
				if (props.containsKey("method") && !props.containsKey("type")) {
					props.put("type", props.get("method"));
					props.remove("method");
				}
			}
		}
		if (sym instanceof GraphSym) {
			float[] range = ((GraphSym) sym).getVisibleYRange();
			props.put("min score", range[0]);
			props.put("max score", range[1]);
		}
		return props;
	}

	/**
	 * Return headings for columns.  If we're laying out
	 * values in a row, then column headings will be the
	 * names associated with each value.  If we're laying
	 * out values in a column, then column headings will
	 * be PROPERTY and then labels for the item whose
	 * values are being presented.
	 * @param name_values - a List containing name-values for a
	 *   one or more Properties
	 * @param props - the list of Properties
	 */
	private static String[] getColumnHeadings(Map[] props) {
		// will contain number of Properties + 1 if by_rows is false
		// will contain number of values if by_rows is true
		String[] col_headings = null;

		// the number of items being described
		int num_items = props.length;
		// columns represent set of properties for a particular entity
		col_headings = new String[num_items + 1];
		col_headings[0] = PROPERTY;
		for (int i = 0; i < num_items; i++) {
			Object id_obj = props[i].get("id");
			String id;
			if (id_obj == null) {
				id = "no ID";
			} else {
				id = id_obj.toString();
			} // in most cases the id already is a String
			col_headings[i + 1] = id;
		}

		return col_headings;
	}

	/**
	 * Build and return rows for the table to be shown in
	 * this PropertySheet.
	 * If there are no Properties to be shown, then returns
	 * default rows.
	 * @param name_values - a List containing name-values for a
	 *   one or more Properties
	 * @param props  the list of Properties
	 */
	private static String[][] buildRows(List<String[]> name_values, Map<String, Object>[] props) {
		int num_vals = name_values.size();
		String[][] rows = new String[num_vals][props.length + 1];
		for (int i = 0; i < num_vals; i++) {
			String[] vals = name_values.get(i);
			rows[i][0] = vals[0];
			System.arraycopy(vals, 1, rows[i], 1, vals.length - 1);
		}
		return rows;
	}

	/**
	 * Show data associated with the given properties.
	 * Uses buildRows() to retrieve ordered
	 * name-value pairs.
	 * @param props  the given Properties
	 * @param preferred_prop_order the preferred order of columns
	 * @param noData the value to use when a property value is null
	 */
	private void showProperties(Map<String, Object>[] props, List<String> preferred_prop_order, String noData) {
		String[][] rows = getPropertiesRow(props,preferred_prop_order, noData);
		String[] col_headings = getColumnHeadings(props);

		propertyChanged(col_headings.length);
		
		TableModel model = new DefaultTableModel(rows, col_headings) {

			public static final long serialVersionUID = 1l;

			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		table.setModel(model);
		table.setDefaultRenderer(Object.class, helper);
		
		sorter = new TableRowSorter<TableModel>(model);
		table.setRowSorter(sorter);

		table.setEnabled(true);  // to allow selection, etc.
		table.setFillsViewportHeight(true);
		this.removeAll();
		this.setLayout(new BorderLayout());
		scroll_pane.setViewportView(table);
		this.add(scroll_pane, BorderLayout.CENTER);
		table.setCellSelectionEnabled(true);

		validate();
		table.getColumnModel().getColumn(0).setMinWidth(100);
		table.getColumnModel().getColumn(0).setPreferredWidth(150);
		table.getColumnModel().getColumn(0).setMaxWidth(200);
	}

	@SuppressWarnings("unchecked")
	public static String[][] getPropertiesRow(SeqSymmetry sym, SeqMapView seqMap){
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		Map<String, Object> props = determineProps(sym, seqMap);
		propList.add(props);

		return getPropertiesRow(propList.toArray(new Map[propList.size()]),prop_order,"");
	}

	@SuppressWarnings("unchecked")
	public static String[][] getGraphPropertiesRowColumn(GraphSym sym, int x, SeqMapView seqMap){
		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();
		Map<String, Object> props = determineProps(sym, seqMap);
		props.put("x coord", x);
		float y = sym.getYCoordFromX(x);
		if (y < 0) {
			props.put("y coord", "no point");
		} else {
			props.put("y coord", y);
		}
		propList.add(props);
		return getPropertiesRow(propList.toArray(new Map[propList.size()]),prop_order,"");
	}

	private static String[][] getPropertiesRow(Map<String, Object>[] props, List<String> preferred_prop_order, String noData){
		List<String[]> name_values = getNameValues(props, noData);
		if (preferred_prop_order != null) {
			name_values = reorderNames(name_values, preferred_prop_order);
		}
		return buildRows(name_values, props);
	}

	/**
	 * take name_values and return a new ArrayList that
	 *  starts with the names found in preferred_ordering in the specified order,
	 *  and then adds entries in name_values that were not found in preferred_ordering.
	 *
	 *  WARNING! this destroys integrity of original name_values!
	 *  also assumes that there are no null entries in name_values
	 *
	 * @param name_values   a List of String[]s
	 * @param preferred_ordering a List of Strings with the preferred order of column names
	 * @return String array of re-ordered names
	 */
	private static List<String[]> reorderNames(List<String[]> name_values, List<String> preferred_ordering) {
		List<String[]> reordered = new ArrayList<String[]>(name_values.size());
		for (String request : preferred_ordering) {
			for (int k = 0; k < name_values.size(); k++) {
				String[] vals = name_values.get(k);
				if (vals != null && vals.length > 0) {
					String name = vals[0];
					if (name.equals(request)) {
						reordered.add(vals);
						name_values.set(k, null);
						break;
					}
				}
			}
		}
		for (String[] name_value : name_values) {
			if (name_value != null) {
				reordered.add(name_value);
			}
		}

		return reordered;
	}


	/**
   * Fills up a Vector with arrays containing names and values
   * for each of the given Properties.
   * e.g., {name,value0,value1,value2,...,valueN} for
   * N different Properties Objects.
   * @param props  the list of Properties derived from SeqFeatures.
   * @param noData  the String value to use to represent cases where
   *   there is no value of the property for a given key
   */
    private static List<String[]> getNameValues(Map<String, Object>[] props, String noData) {
		List<String[]> result = new ArrayList<String[]>();
		// collect all possible names from the given Properties
		int num_props = props.length;
		Map<String, String[]> rows_thus_far = new HashMap<String, String[]>();
		for (int i = 0; i < num_props; i++) {
			for (String name : props[i].keySet()) {
				if (name != null && rows_thus_far.containsKey(name)) {
						continue;
				}		

				String name_value[] = new String[num_props + 1];
				name_value[0] = name;
				for (int j = 0; j < props.length; j++) {
					Object val = props[j].get(name);
					val = (val == null ? noData : val);
					// if val is a List for multivalued property, rely on toString() to convert to [item1, item2, etc.]
					//   string representation
					name_value[j + 1] = val.toString();
				}
				rows_thus_far.put(name, name_value);
			}
		}

		result.addAll(rows_thus_far.values());
		
		return result;
	}

	private void propertyChanged(int prop_displayed){
		for(PropertyListener pl : propertyListeners){
			pl.propertyDisplayed(prop_displayed);
		}
	}

	public interface PropertyListener{
		public void propertyDisplayed(int prop_displayed);
	}
}


