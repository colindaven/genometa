package com.affymetrix.genometry.servlets;

final class Das2Coords {
	private final String urid;
	private final String authority;
	private final String taxid;
	private final String version;
	private final String source;
	private final String test_range;

	Das2Coords(String urid, String authority, String taxid,
			String version, String source, String test_range) {
		this.urid = urid;
		this.authority = authority;
		this.taxid = taxid;
		this.version = version;
		this.source = source;
		this.test_range = test_range;
	}

	String getURI() { return urid; }
	String getAuthority() { return authority; }
	String getTaxid() { return taxid; }
	String getVersion() { return version; }
	String getSource() { return source; }
	String getTestRange() { return test_range; }

}
