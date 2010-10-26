package com.affymetrix.genoviz.swing;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

public class DragDropTree extends JTree implements DragSourceListener, DropTargetListener, DragGestureListener {

	private final DragSource source;
	private TransferableTreeNode transferable;
	private DefaultMutableTreeNode oldNode;
	private final boolean DEBUG = false;
	
	public DragDropTree() {
		super();
		
		source = new DragSource();
		this.setDropTarget(new DropTarget(this, this));
		source.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
	}

	/*
	 * Drag Gesture Handler
	 */
	public void dragGestureRecognized(DragGestureEvent dge) {
		TreePath path = getSelectionPath();
		if ((path == null) || (path.getPathCount() <= 1)) {
			// We can't move the root node or an empty selection
			return;
		}
		oldNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		transferable = new TransferableTreeNode(path);
		//source.startDrag(dge, DragSource.DefaultMoveNoDrop, transferable, this);

		// If you support dropping the node anywhere, you should probably
		// start with a valid move cursor:
		source.startDrag(dge, DragSource.DefaultMoveDrop, transferable, this);
	}

	/*
	 * Source Drag Event Handlers
	 */
	public void dragEnter(DragSourceDragEvent dsde) {
	}

	public void dragExit(DragSourceEvent dse) {
	}

	public void dragOver(DragSourceDragEvent dsde) {
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
		if(DEBUG){
			System.out.println("Action: " + dsde.getDropAction());
			System.out.println("Target Action: " + dsde.getTargetActions());
			System.out.println("User Action: " + dsde.getUserAction());
		}
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		if(DEBUG){
			System.out.println("Drop Action: " + dsde.getDropAction());
		}
		
		if (dsde.getDropSuccess() && (dsde.getDropAction() == DnDConstants.ACTION_MOVE)) {
			((DefaultTreeModel) getModel()).removeNodeFromParent(oldNode);
		}

	}

	/*
	 * Target Drag Event Handlers
	 */

	public void dragEnter(DropTargetDragEvent dtde) {
		dtde.acceptDrag(dtde.getDropAction());
	}

	public void dragOver(DropTargetDragEvent dtde) {
		dtde.acceptDrag(dtde.getDropAction());
	}

	public void dragExit(DropTargetEvent dte) {
	}

	public void dropActionChanged(DropTargetDragEvent dtde) {
	}

	public void drop(DropTargetDropEvent dtde) {
		Point pt = dtde.getLocation();
		DropTargetContext dtc = dtde.getDropTargetContext();
		JTree tree = (JTree) dtc.getComponent();
		TreePath parentpath = tree.getClosestPathForLocation(pt.x, pt.y);
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) parentpath.getLastPathComponent();


		try {
			Transferable tr = dtde.getTransferable();
			DataFlavor[] flavors = tr.getTransferDataFlavors();
			for (int i = 0; i < flavors.length; i++) {
				if (tr.isDataFlavorSupported(flavors[i])) {
					dtde.acceptDrop(dtde.getDropAction());
					TreePath p = (TreePath) tr.getTransferData(flavors[i]);
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) p.getLastPathComponent();
					DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
					if(parent.isLeaf()){
						MutableTreeNode actualparent = (MutableTreeNode) parent.getParent();
						int index = model.getIndexOfChild(actualparent, parent);
						model.insertNodeInto(node, actualparent, index + 1);
						dtde.dropComplete(true);
						return;
					}
					model.insertNodeInto(node, parent, 0);
					dtde.dropComplete(true);
					return;
				}
			}
			dtde.rejectDrop();
		} catch (Exception e) {
			e.printStackTrace();
			dtde.rejectDrop();
		}
	}

	//TransferableTreeNode.java
	//A Transferable TreePath to be used with Drag & Drop applications.
	//
	class TransferableTreeNode implements Transferable {

		public DataFlavor TREE_PATH_FLAVOR = new DataFlavor(TreePath.class, "Tree Path");
		DataFlavor flavors[] = {TREE_PATH_FLAVOR};
		TreePath path;

		public TransferableTreeNode(TreePath tp) {
			path = tp;
		}

		public synchronized DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return (flavor.getRepresentationClass() == TreePath.class);
		}

		public synchronized Object getTransferData(DataFlavor flavor)
				throws UnsupportedFlavorException, IOException {
			if (isDataFlavorSupported(flavor)) {
				return (Object) path;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}
}

