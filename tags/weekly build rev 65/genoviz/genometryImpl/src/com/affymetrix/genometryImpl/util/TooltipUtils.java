package com.affymetrix.genometryImpl.util;


public final class TooltipUtils {

	public static final int MAX_TOOLTIP_LENGTH = 25;
	public static final TooltipObject[] default_tooltip = {
		new TooltipObject("name", true),
		new TooltipObject("id", true),
		new TooltipObject("chromosome", true),
		new TooltipObject("start", true),
		new TooltipObject("end", true),
		new TooltipObject("length", true),
		new TooltipObject("type", true),
		new TooltipObject("residues", true),
		new TooltipObject("VN", true),
		new TooltipObject("score", true),
		new TooltipObject("SEQ", true),
		new TooltipObject("SM", true),
		new TooltipObject("baseQuality", true),
		new TooltipObject("cigar", true),
		new TooltipObject("XA", true),
		new TooltipObject("forward", true),
		new TooltipObject("NM", true),
		new TooltipObject("method", true),
		new TooltipObject("MD", true),
		new TooltipObject("CL", true),
	};


	public static void setTooltipName(int i, String name) {
		default_tooltip[i].setName(name);
	}
	public static String getTooltipName(int i) {
		return default_tooltip[i].getName();
	}

	public static void setTooltipShow(int i, boolean show) {
		default_tooltip[i].setShow(show);
	}
	public static boolean isTooltipShow(int i) {
		return default_tooltip[i].isShow();
	}

	public static boolean isShowByName(String name) {
		for(int i = 0; i < default_tooltip.length; i++) {
			if(default_tooltip[i].getName().equals(name)) {
				return default_tooltip[i].isShow();
			}
		}
		return false;
	}

	public static int countTooltips() {
		return default_tooltip.length;
	}

}
