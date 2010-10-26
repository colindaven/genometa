package com.affymetrix.genoviz.swing;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A TableCellRenderer for showing a boolean value with a JCheckBox.  This is
 * necessary as Java's internal TableCellRenderer for Boolean values has many
 * limitations. Improvements over Java's implementation:
 * <ul>
 *   <li>Handles selection correctly</li>
 *   <li>Handles focus correctly</li>
 *   <li>Handles alternate row colors</li>
 *   <li>Greys out CheckBox when read-only</li>
 * </ul>
 *
 * @version $Id: BooleanTableCellRenderer.java 5171 2010-02-05 01:01:53Z sgblanch $
 */
public final class BooleanTableCellRenderer extends DefaultTableCellRenderer {
	public static final long serialVersionUID = 1l;
	private final JCheckBox checkbox;

	public BooleanTableCellRenderer() {
		super();
		this.checkbox = new JCheckBox();
		this.checkbox.setHorizontalAlignment(CENTER);
		this.checkbox.setVerticalAlignment(CENTER);
		this.checkbox.setOpaque(true);
		this.checkbox.setBorderPainted(true);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		this.checkbox.setEnabled(table.isCellEditable(row, column));
		this.checkbox.setSelected((value != null && (Boolean) value));
		this.checkbox.setForeground(this.getForeground());
		this.checkbox.setBackground(new Color(this.getBackground().getRGB()));
		this.checkbox.setBorder(this.getBorder());

		return checkbox;
	}
}
