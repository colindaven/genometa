package com.affymetrix.genometryImpl.util;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple helper function to build URLs with query components from a base URL.
 * 
 * @author sgblanch
 * @version $Id: QueryBuilder.java 6838 2010-09-02 19:54:19Z jnicol $
 */
public class QueryBuilder {
	private final Map<String, String> parameters = new LinkedHashMap<String, String>();
	private final String u;

	public QueryBuilder(String u) {
		this.u = u;
	}

	public void add(String key, String value) {
		parameters.put(GeneralUtils.URLEncode(key), GeneralUtils.URLEncode(value));
	}

	public URI build() {
		StringBuilder query = new StringBuilder(u);

		for (Map.Entry<String, String> parameter : parameters.entrySet()) {
			query.append(parameter.getKey());
			query.append("=");
			query.append(parameter.getValue());
			query.append(";");
		}

		return URI.create(query.toString());
	}
}
