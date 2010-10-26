package com.affymetrix.genometryImpl.style;

import java.util.HashMap;
import java.util.Map;


/**Contributed by Ido Tamir.*/
public enum GraphType {
	LINE_GRAPH("Line", 1),
	BAR_GRAPH("Bar", 2),
	DOT_GRAPH("Dot", 3),
	MINMAXAVG("Min_Max_Avg", 4),
	STAIRSTEP_GRAPH("Stairstep", 5),
	HEAT_MAP("HeatMap", 6);

	private String humanReadable;
	private static Map<String,GraphType> humanReadable2number;

	static {
		humanReadable2number = new HashMap<String,GraphType>();
		for( GraphType type : values()){
			humanReadable2number.put(type.humanReadable, type);
		}
	}


	private GraphType(String humanReadable, int number){
		this.humanReadable = humanReadable;
	}

	public static GraphType fromString(String humanReadable){
		GraphType nr = humanReadable2number.get(humanReadable);
		if(nr != null){
			return nr;
		}
		return GraphType.LINE_GRAPH;
	}

	@Override
	public String toString(){
		return humanReadable;
	}
}
