package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.igb.IGB;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

/**
 * Model for table of features.
 */
public final class FeaturesTableModel extends AbstractTableModel implements ChangeListener{
	private static final String[] columnNames = {"","Choose Load Mode", "Data Set","Data Source", "x"};
	private final Map<String, LoadStrategy> reverseLoadStrategyMap;  // from friendly string to enum
	static final int REFRESH_FEATURE_COLUMN = 0;
	static final int LOAD_STRATEGY_COLUMN = 1;
	static final int FEATURE_NAME_COLUMN = 2;
	private static final int SERVER_NAME_COLUMN = 3;
	static final int DELETE_FEATURE_COLUMN = 4;
	private List<GenericFeature> features;
	private final GeneralLoadView glv;
	private final static featureTableComparator visibleFeatureComp = new featureTableComparator();
	static final String DELETE_COMMAND = "delete";
	static final String REFRESH_COMMAND = "refresh";

	FeaturesTableModel(GeneralLoadView glv) {
		this.glv = glv;
		this.features = null;

		// Here we map the friendly string back to the LoadStrategy.
		this.reverseLoadStrategyMap = new HashMap<String, LoadStrategy>(3);
		for (LoadStrategy strategy : EnumSet.allOf(LoadStrategy.class)) {
			this.reverseLoadStrategyMap.put(strategy.toString(), strategy);
		}
	}

	void clearFeatures() {
		if (this.features != null) {
			this.features.clear();
		}
		this.fireTableDataChanged();
	}

	void setFeatures(List<GenericFeature> features) {
		this.features = features;
		this.fireTableDataChanged();
	}

	/**
	 * Only want to display features with visible attribute set to true.
	 * @param features
	 * @return list of visible features
	 */
	static List<GenericFeature> getVisibleFeatures(List<GenericFeature> features) {
		if (features == null) {
			return null;
		}
		List<GenericFeature> visibleFeatures = new ArrayList<GenericFeature>();
		for (GenericFeature gFeature : features) {
			if (gFeature.isVisible()) {
				visibleFeatures.add(gFeature);
			}
		}

		Collections.sort(visibleFeatures,visibleFeatureComp);

		// Also sort these features so the features to be loaded are at the top.

		return visibleFeatures;
	}

	private final static class featureTableComparator implements Comparator<GenericFeature> {

		public int compare(GenericFeature feature1, GenericFeature feature2) {
			if (feature1.loadStrategy != feature2.loadStrategy) {
				return (feature1.loadStrategy.compareTo(feature2.loadStrategy));
			}
			if (feature1.featureName.compareTo(feature2.featureName) != 0) {
				return feature1.featureName.compareTo(feature2.featureName);
			}
			return feature1.gVersion.gServer.serverType.compareTo(
					feature2.gVersion.gServer.serverType);
		}
	}



	GenericFeature getFeature(int row) {
		return (getRowCount() <= row) ? null : features.get(row);
	}

	private int getRow(GenericFeature feature) {
		return (features == null) ? -1 : features.indexOf(feature);
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return (features == null) ? 0 : features.size();
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		if (features == null || features.isEmpty()) {
			// Indicate to user that there's no data.
			if (row == 0 && col == 2) {
				return "No feature data found";
			}
			return "";
		}
		if(getFeature(row) == null) {
			return "";
		}
		GenericFeature gFeature = features.get(row);
		switch (col) {
			case REFRESH_FEATURE_COLUMN:
				return REFRESH_COMMAND;
			case LOAD_STRATEGY_COLUMN:
				// return the load strategy
				return gFeature.loadStrategy.toString();
			case FEATURE_NAME_COLUMN:
				// the friendly feature name removes slashes.  Clip it here.
				if (gFeature.gVersion.gServer.serverType == ServerType.QuickLoad) {
					return LoadUtils.stripFilenameExtensions(gFeature.featureName);
				}
				return gFeature.featureName;
			case SERVER_NAME_COLUMN:
				// return the friendly server name
				return gFeature.gVersion.gServer.serverName + " (" + gFeature.gVersion.gServer.serverType + ")";
			case DELETE_FEATURE_COLUMN:
				return DELETE_COMMAND;
				
			default:
				System.out.println("Shouldn't reach here: " + row + " " + col);
				return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		if ((getValueAt(0, c)) == null) {
			System.out.println("Null Reference ERROR: column " + c);
		}
		return getValueAt(0, c).getClass();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		if (col == DELETE_FEATURE_COLUMN || col == REFRESH_FEATURE_COLUMN)
			return true;
		
		if (col != LOAD_STRATEGY_COLUMN) {
			return false;
		}
		if(getFeature(row) == null){
			return false;
		}
		// This cell is only editable if the feature isn't already fully loaded.
		return (getFeature(row).loadStrategy != LoadStrategy.GENOME);
	}

	@Override
	public void setValueAt(Object value, int row, int col) {

		if(value == null || (col != LOAD_STRATEGY_COLUMN &&
				col != DELETE_FEATURE_COLUMN && col != REFRESH_FEATURE_COLUMN)){
			return;
		}

		GenericFeature gFeature = getFeature(row);

		if(gFeature == null){
			return;
		}

		if (col == DELETE_FEATURE_COLUMN){
			if(IGB.confirmPanel("Really remove entire " + gFeature.featureName + " data set ?")){
				GeneralLoadView.removeFeature(gFeature);
			}
			return;
		}

		if (col == REFRESH_FEATURE_COLUMN) {
			if(gFeature.loadStrategy != LoadStrategy.NO_LOAD && gFeature.loadStrategy != LoadStrategy.GENOME)
				GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
			return;
		}

		if (gFeature.loadStrategy == LoadStrategy.GENOME) {
			return;	// We can't change strategies once we've loaded the entire genome.
		}
		String valueString = value.toString();
		if (!gFeature.loadStrategy.toString().equals(valueString)) {
			// strategy changed.  Update the feature object.
			gFeature.loadStrategy = this.reverseLoadStrategyMap.get(valueString);
			updatedStrategy(row, col, gFeature);
		}
	}

	/**
	 * The strategy was changed.  Update the table, and if necessary, load the annotations and change the button statuses.
	 * @param row
	 * @param col
	 * @param gFeature
	 */
	private void updatedStrategy(int row, int col, GenericFeature gFeature) {
		fireTableCellUpdated(row, col);

		if (gFeature.loadStrategy == LoadStrategy.GENOME) {
			GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
		}

		//  Whatever feature strategy changed, it may have affected
		// the enable status of the "load visible" button
		this.glv.changeVisibleDataButtonIfNecessary(features);
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
		if (src instanceof GenericFeature) {
			int row = getRow((GenericFeature) src);
			if (row >= 0) {  // if typestate is present in table, then send notification of row change
				fireTableRowsUpdated(row, row);
			}
		}
	}

}
