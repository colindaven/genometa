package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genoviz.swing.ButtonTableCellEditor;
import com.affymetrix.genoviz.swing.LabelTableCellRenderer;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.RefreshDataAction;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Icon;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * A table with two customizations:
 * 1.  An always-visible combo box. For a user, this differentiates the field from a text box, and thus indicates they have a choice.
 * 2.  Different combo box elements per row.  This allows different behavior per server type.
 */
public final class TableWithVisibleComboBox {
	private static TableRowSorter<FeaturesTableModel> sorter;
	private static final JComboBoxToolTipRenderer comboRenderer = new JComboBoxToolTipRenderer();
	static final Icon refresh_icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Refresh16.gif");
	static final Icon delete_icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Delete16.gif");
	
	/**
	 * Set the columns to use the ComboBox DAScb and renderer (which also depends on the row/server type)
	 * @param table
	 * @param column
	 * @param enabled
	 */
	static void setComboBoxEditors(JTableX table, boolean enabled) {
		comboRenderer.setToolTipEntry(LoadStrategy.NO_LOAD.toString(), IGBConstants.BUNDLE.getString("noLoadCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.VISIBLE.toString(), IGBConstants.BUNDLE.getString("visibleCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.CHROMOSOME.toString(), IGBConstants.BUNDLE.getString("chromosomeCBToolTip"));
		comboRenderer.setToolTipEntry(LoadStrategy.GENOME.toString(), IGBConstants.BUNDLE.getString("genomeCBToolTip"));
		FeaturesTableModel ftm = (FeaturesTableModel) table.getModel();
		sorter = new TableRowSorter<FeaturesTableModel>(ftm);
		table.setRowSorter(sorter);

		int featureSize = ftm.getRowCount();
		RowEditorModel choices = new RowEditorModel(featureSize);
		RowEditorModel action = new RowEditorModel(featureSize);

		// tell the JTableX which RowEditorModel we are using
		table.setRowEditorModel(FeaturesTableModel.LOAD_STRATEGY_COLUMN, choices);
		table.setRowEditorModel(FeaturesTableModel.DELETE_FEATURE_COLUMN, action);
		table.setRowEditorModel(FeaturesTableModel.REFRESH_FEATURE_COLUMN, action);

		for (int row = 0; row < featureSize; row++) {
			GenericFeature gFeature = ftm.getFeature(row);
			JComboBox featureCB = new JComboBox(gFeature.getLoadChoices().toArray());
			featureCB.setRenderer(comboRenderer);
			featureCB.setEnabled(true);
			DefaultCellEditor featureEditor = new DefaultCellEditor(featureCB);
			choices.addEditorForRow(row, featureEditor);

			ButtonTableCellEditor buttonEditor = new ButtonTableCellEditor(gFeature);
			action.addEditorForRow(row, buttonEditor);
		}

		TableColumn c = table.getColumnModel().getColumn(FeaturesTableModel.LOAD_STRATEGY_COLUMN);
		c.setCellRenderer(new ColumnRenderer());
		((JComponent) c.getCellRenderer()).setEnabled(enabled);

		c = table.getColumnModel().getColumn(FeaturesTableModel.DELETE_FEATURE_COLUMN);
		c.setCellRenderer(new LabelTableCellRenderer(delete_icon, true));
		c.setHeaderRenderer(new LabelTableCellRenderer(delete_icon, true));

		c = table.getColumnModel().getColumn(FeaturesTableModel.REFRESH_FEATURE_COLUMN);
		c.setCellRenderer(new LabelTableCellRenderer(refresh_icon, true));
		c.setHeaderRenderer(new LabelTableCellRenderer(refresh_icon, true));
	}

  static final class ColumnRenderer extends JComponent implements TableCellRenderer {

    private final JComboBox comboBox;
    private final JTextField textField;	// If an entire genome is loaded in, change the combo box to a text field.

    public ColumnRenderer() {
      comboBox = new JComboBox();
	  comboBox.setRenderer(comboRenderer);
      comboBox.setBorder(null);

	  textField = new JTextField(LoadStrategy.GENOME.toString());
	  textField.setToolTipText(IGBConstants.BUNDLE.getString("genomeCBToolTip"));	// only for whole genome
      textField.setBorder(null);
    }

    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

      if (((String) value).equals(textField.getText())) {
        return textField;
      } else {
        comboBox.removeAllItems();
        comboBox.addItem(value);
        return comboBox;
      }
    }
  }
}

/**
 * This maps a row to a specific editor.
 */
class RowEditorModel {

  private final Map<Integer, TableCellEditor> row2Editor;

  RowEditorModel(int size) {
    row2Editor = new HashMap<Integer, TableCellEditor>(size);
  }

  void addEditorForRow(int row, TableCellEditor e) {
    row2Editor.put(Integer.valueOf(row), e);
  }

  TableCellEditor getEditor(int row) {
    return row2Editor.get(Integer.valueOf(row));
  }
}

/**
 * A JTable with a RowEditorModel.
 */
class JTableX extends JTable implements MouseListener {
  protected String[] columnToolTips = {"Refresh All",
                                       "Load Strategy",
                                       "Feature Name",
                                       "Feature Location",
                                       "Delete All"};

  private final Map<Integer, RowEditorModel> rmMap;

  public JTableX(TableModel tm) {
    super(tm);
	getTableHeader().addMouseListener(this);
    rmMap = new HashMap<Integer, RowEditorModel>();
  }

  void setRowEditorModel(int column, RowEditorModel rm) {
    this.rmMap.put(column, rm);
  }

  @Override
  public TableCellEditor getCellEditor(int row, int col) {
    if (rmMap != null) {
      TableCellEditor tmpEditor = rmMap.get(col).getEditor(row);
	  if (tmpEditor != null) {
		  return tmpEditor;
	  }
    }
    return super.getCellEditor(row, col);
  }

   @Override
   public TableCellRenderer getCellRenderer(int row, int column) {
	   if(column == FeaturesTableModel.REFRESH_FEATURE_COLUMN){
		   FeaturesTableModel ftm = (FeaturesTableModel) getModel();
		   GenericFeature feature = ftm.getFeature(row);
		   boolean enabled = (feature.loadStrategy != LoadStrategy.NO_LOAD && feature.loadStrategy != LoadStrategy.GENOME);
		   return new LabelTableCellRenderer(TableWithVisibleComboBox.refresh_icon, enabled);
	   }else if(column == FeaturesTableModel.LOAD_STRATEGY_COLUMN){
		   return new TableWithVisibleComboBox.ColumnRenderer();
	   }else if(column == FeaturesTableModel.DELETE_FEATURE_COLUMN){
		   return new LabelTableCellRenderer(TableWithVisibleComboBox.delete_icon, true);
	   }

	   return super.getCellRenderer(row,column);
   }

   @Override
   public String getToolTipText(MouseEvent e) {
	   String tip = null;
	   java.awt.Point p = e.getPoint();
       int rowIndex = rowAtPoint(p);
       int colIndex = columnAtPoint(p);
       int realColumnIndex = convertColumnIndexToModel(colIndex);
	   FeaturesTableModel ftm = (FeaturesTableModel) getModel();
	   GenericFeature feature = ftm.getFeature(rowIndex);
	   String featureName = feature.featureName;

	   switch(realColumnIndex){
		   case FeaturesTableModel.REFRESH_FEATURE_COLUMN:
			   if(feature.loadStrategy != LoadStrategy.NO_LOAD)
				   tip = "Refresh " + featureName;
			   else
				   tip = "Change load strategy to refresh " + featureName;
			   break;
			   
		   case FeaturesTableModel.LOAD_STRATEGY_COLUMN:
			   if(feature.loadStrategy != LoadStrategy.GENOME)
				   tip = "Change load strategy for " + featureName;
			   else
				   tip = "Cannot change load strategy for " + featureName;
			   break;
			   
		   case FeaturesTableModel.DELETE_FEATURE_COLUMN:
			   tip = "Delete " + featureName;
			   break;
			   
		   default:				   
	   }

	   return tip;
   }

	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realIndex = columnModel.getColumn(index).getModelIndex();
				return columnToolTips[realIndex];
			}
		};
	}

	public void mouseReleased(MouseEvent e) {
		java.awt.Point p = e.getPoint();
		int index = columnModel.getColumnIndexAtX(p.x);
		int realIndex = columnModel.getColumn(index).getModelIndex();

		if (FeaturesTableModel.REFRESH_FEATURE_COLUMN == realIndex
				&& GeneralLoadView.getIsDisableNecessary()) {

			RefreshDataAction.getAction().actionPerformed(null);

		} else if (FeaturesTableModel.DELETE_FEATURE_COLUMN == realIndex) {
			FeaturesTableModel ftm = (FeaturesTableModel) getModel();
			int featureSize = ftm.getRowCount();
			
			if (featureSize > 0 && IGB.confirmPanel("Really remove all data sets ?")) {
				for (int row = 0; row < featureSize; row++) {
					GeneralLoadView.removeFeature(ftm.getFeature(row));
				}
			}
		}
	}

	public void mouseClicked(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
}
