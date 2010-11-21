package com.affymetrix.genoviz.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.TransferHandler;

/**
 *
 * @author hiralv
 */
public abstract class FileDropHandler extends TransferHandler {

	@Override
	public boolean canImport(TransferSupport support) {
		return (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || support.isDataFlavorSupported(DataFlavor.stringFlavor));

	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}

		Transferable t = support.getTransferable();
		try {
			if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				for (File f : files) {
					openFileAction(f);
				}
				return true;
			}

			// Workaround for Linux -- see http://bugs.sun.com/view_bug.do?bug_id=4899516, for example
			DataFlavor uriListFlavor = null;
			String data = null;
			try {
				uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
				if (uriListFlavor != null && support.isDataFlavorSupported(uriListFlavor)) {
					data = (String) t.getTransferData(uriListFlavor);
				}
			} catch (ClassNotFoundException ex) {
				// class not found?
			}
			if (data != null) {
				List<URL> urls = textURLList(data);
				if (urls != null) {
					for (URL url : urls) {
						openURLAction(url.toString());
					}
					return true;
				}
			}

			// Last possible action
			String url = (String) t.getTransferData(DataFlavor.stringFlavor);
			openURLAction(url);
		} catch (UnsupportedFlavorException ex) {
			return false;
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	private static List<URL> textURLList(String data) {
        List<URL> list = new ArrayList<URL>();
        for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
            String s = st.nextToken();
            if (s.charAt(0) == '#') {
                // the line is a comment (as per the RFC 2483)
                continue;
            }
            try {
                URI uri = new URI(s);
                list.add(uri.toURL());
            } catch (MalformedURLException ex) {
				// the URL is not well-formed from the URI?  Unlikely
			} catch (java.net.URISyntaxException e) {
                // malformed URI
            } catch (IllegalArgumentException e) {
                // the URI is not a valid 'file:' URI
            }
        }
        return list;
    }

	abstract public void openFileAction(File f);

	abstract public void openURLAction(String url);
}
