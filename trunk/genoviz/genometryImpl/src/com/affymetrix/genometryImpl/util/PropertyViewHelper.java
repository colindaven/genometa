package com.affymetrix.genometryImpl.util;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author hiralv
 */
public class PropertyViewHelper extends DefaultTableCellRenderer implements
			MouseListener, MouseMotionListener {

		private final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		private final Cursor defaultCursor = null;
		private final JTable table;

		public PropertyViewHelper(JTable table){
			this.table = table;
			table.addMouseListener(this);
			table.addMouseMotionListener(this);
		}
		
		@Override
		public Component getTableCellRendererComponent (JTable table,
				Object obj, boolean isSelected, boolean hasFocus, int row, int column){

			if(isURLField(row,column)){

				String url = "<html> <a href='" + (String)obj + "'>" +
						(String)obj + "</a> </html>)";

				return super.getTableCellRendererComponent (table, url,
						isSelected, hasFocus, row, column);
			}

			return super.getTableCellRendererComponent (table, obj, isSelected,
					hasFocus, row, column);
		}

		@Override
		public void mouseClicked(MouseEvent e){

			Point p = e.getPoint();
			int row = table.rowAtPoint(p);
			int column = table.columnAtPoint(p);

			if (isURLField(row,column)) {
				GeneralUtils.browse((String) table.getValueAt(row, column));
			}

		}
		public void mouseMoved(MouseEvent e) {
			Point p = e.getPoint();
			int row = table.rowAtPoint(p);
			int column = table.columnAtPoint(p);

			if(isURLField(row,column)){
				table.setCursor(handCursor);
			}else if(table.getCursor() != defaultCursor) {
				table.setCursor(defaultCursor);
			}
		}

		public void mouseDragged(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}

		private boolean isURLField(int row, int column){

			if(row > table.getRowCount() || column > table.getColumnCount() ||
					row < 0 || column < 0)
				return false;
			
			String value = (String) table.getValueAt(row, column);

			if(value.length() <= 0)
				return false;

			if(value.startsWith("http://") || value.startsWith("https://"))
				return true;

			return false;
		};
}
