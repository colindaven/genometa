/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.util;

/**
 *
 * @author Nisl
 */
public class TooltipObject {
	private String _name;
	private boolean _show;

	public TooltipObject(String name, boolean show) {
		_name = name;
		_show = show;
	}

	public void setName(String name) {
		_name = name;
	}
	public String getName() {
		return _name;
	}

	public void setShow(boolean show) {
		_show = show;
	}
	public boolean isShow() {
		return _show;
	}
}
