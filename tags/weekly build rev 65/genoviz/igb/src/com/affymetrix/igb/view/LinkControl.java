package com.affymetrix.igb.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.util.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.prefs.WebLink;

final class LinkControl implements ContextualPopupListener {
	public void popupNotify(JPopupMenu popup, List selected_syms, SeqSymmetry primary_sym) {
		if (primary_sym == null || selected_syms.size() != 1) {
			return;
		}

		Map<String, String> menu_items = new LinkedHashMap<String, String>(); // map of menu url->name, or url -> url if there is no name

		// DAS files can contain links for each individual feature.
		// These are stored in the "link" property
		Object links = null;
		if (primary_sym instanceof SymWithProps) {
			links = ((SymWithProps) primary_sym).getProperty("link");
			if (links != null) {
				generateMenuItemsFromLinks(links, primary_sym, menu_items);
			}
		}

		generateMenuItemsFromWebLinks(primary_sym, menu_items);

		makeMenuItemsFromMap(popup, menu_items);

	}

	@SuppressWarnings("unchecked")
	private void generateMenuItemsFromLinks(Object links, SeqSymmetry primary_sym, Map<String, String> menu_items) {
		if (links instanceof String) {
			Object link_names = null;
			if (primary_sym instanceof SymWithProps) {
				link_names = ((SymWithProps) primary_sym).getProperty("link_name");
			}
			String url = (String) links;
			url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
			if (link_names instanceof String) {
				menu_items.put(url, (String) link_names);
			} else {
				menu_items.put(url, url);
			}
		} else if (links instanceof List) {
			List<String> urls = (List<String>) links;
			for (String url : urls) {
				url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
				menu_items.put(url, url);
			}
		} else if (links instanceof Map) {
			Map<String, String> name2url = (Map<String, String>) links;
			for (Map.Entry<String, String> entry : name2url.entrySet()) {
				String name = entry.getKey();
				String url = entry.getValue();
				url = WebLink.replacePlaceholderWithId(url, primary_sym.getID());
				menu_items.put(url, name);
			}
		}
	}

	private void generateMenuItemsFromWebLinks(SeqSymmetry primary_sym, Map<String, String> menu_items) {
		// Most links come from matching the tier name (i.e. method)
		// to a regular expression.
		String method = BioSeq.determineMethod(primary_sym);
		// by using a Map to hold the urls, any duplicated urls will be filtered-out.
		for (WebLink webLink : WebLink.getWebLinks(method, primary_sym.getID())) {
			// Generally, just let any link replace an existing link that has the same URL.
			// But, if the new one has no name, and the old one does, then keep the old one.
			String new_name = webLink.getName();
			String url = webLink.getURLForSym(primary_sym);
			String old_name = menu_items.get(url);
			if (old_name == null || "".equals(old_name)) {
				menu_items.put(url, new_name);
			}
		}
	}

	private static void makeMenuItemsFromMap(JPopupMenu popup, Map<String, String> urls) {
		if (urls.isEmpty()) {
			return;
		}

		if (urls.size() == 1) {
			for (Map.Entry<String, String> entry : urls.entrySet()) {
				String url = entry.getKey();
				String name = entry.getValue();
				if (name == null || name.equals(url)) {
					name = "Get more info";
				}

				JMenuItem mi = makeMenuItem(name, url);
				mi.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
				popup.add(mi);
			}
		} else {
			JMenu linkMenu = new JMenu("Get more info");
			linkMenu.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
			popup.add(linkMenu);

			for (Map.Entry<String, String> entry : urls.entrySet()) {
				String url = entry.getKey();
				String name = entry.getValue();
				if (name == null || name.equals(url)) {
					name = "Unnamed link to web";
				}
				JMenuItem mi = makeMenuItem(name, url);
				linkMenu.add(mi);
			}
		}
	}

	private static JMenuItem makeMenuItem(String name, final String url) {
		JMenuItem linkMI = new JMenuItem(name);
		if (url != null) {
			linkMI.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					GeneralUtils.browse(url);
				}
			});
		}
		return linkMI;
	}
}
