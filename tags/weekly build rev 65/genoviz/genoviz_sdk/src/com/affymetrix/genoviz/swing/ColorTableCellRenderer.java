package com.affymetrix.genoviz.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author sgblanch
 * @version $Id: ColorTableCellRenderer.java 5172 2010-02-05 01:07:39Z sgblanch $
 */
public class ColorTableCellRenderer extends DefaultTableCellRenderer {
	public static final long serialVersionUID = 1l;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		this.setBackground(null);
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		this.setBorder(new StackedBorder(this.getInsets(), this.getBorder(), this.getBackground()));
		this.setBackground((Color) value);

		return this;
	}

	@Override
	public void setValue(Object value) { }

	/**
	 * Class to stack borders.  Adds an opaque MatteBorder behind another
	 * (tranparent) border.
	 */
	private static class StackedBorder extends MatteBorder {
		public static final long serialVersionUID = 1l;
		private Border border;

		StackedBorder(Insets borderInsets, Border b, Color color) {
			super(borderInsets, color);
			this.border = b;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			super.paintBorder(c, g, x, y, width, height);
			border.paintBorder(c, g, x, y, width, height);
		}
	}
}
