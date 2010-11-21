package com.affymetrix.genometryImpl.util;

public final class LoadUtils {
	public static enum LoadStrategy {
		NO_LOAD ("Don't Load"),
		VISIBLE ("Region In View"),
		CHROMOSOME ("Whole Chromosome"),
		GENOME ("Whole Genome");

		private String name;

		LoadStrategy(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	public static enum LoadStatus {
		UNLOADED ("Unloaded"),
		LOADING ("Loading"),
		LOADED ("Loaded");

		private String name;

		LoadStatus(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	public static enum ServerType {
		DAS2 ("DAS2"),
		DAS ("DAS"),
		QuickLoad ("QuickLoad"),
		LocalFiles ("Local Files");

		private String name;

		ServerType(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	public static enum ServerStatus {
		NotInitialized ("Not initialized"),
		Initialized ("Initialized"),
		NotResponding ("Not responding");

		private String name;

		ServerStatus(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};

	/**
	 * Used to give a friendly name for QuickLoad features.
	 * @param name
	 * @return name
	 */
	public static String stripFilenameExtensions(final String name) {
		// Remove ending .gz or .zip extension.
		if (name.endsWith(".gz")) {
			return stripFilenameExtensions(name.substring(0, name.length() -3));
		}
		if (name.endsWith(".zip")) {
			return stripFilenameExtensions(name.substring(0, name.length() -4));
		}

		if (name.indexOf('.') > 0) {
			return name.substring(0, name.lastIndexOf('.'));
		}
		return name;
	}
}
