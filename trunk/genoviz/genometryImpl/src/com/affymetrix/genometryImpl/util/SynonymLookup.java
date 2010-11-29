package com.affymetrix.genometryImpl.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *  A way of mapping synonyms to each other.
 *
 * @version $Id: SynonymLookup.java 6684 2010-08-17 18:01:39Z hiralv $
 */
public final class SynonymLookup {
	/**
	 * Default behaviour of case sensitivity for synonym lookups. If true,
	 * searches will be cases sensitive.  The default is {@value}.
	 */
	private static final boolean DEFAULT_CS = true;

	/**
	 * Default behaviour for stripping '_random' from synonyms.  If true,
	 * the string '_random' will be removed from the end of synonyms.  The
	 * default value is {@value}.
	 */
	private static final boolean DEFAULT_SR = false;
	
	/**
	 * Regular Expression used to split fields of the synonym file.  The
	 * synonyms file is split on one or more tab characters.
	 */
	private static final Pattern LINE_REGEX = Pattern.compile("\\t+");

	/** The default instance of this class, used by most code for synonym lookups. */
	private static final SynonymLookup DEFAULT_LOOKUP = new SynonymLookup();

	/** The default instance of this class for chromosome, used by most code for synonym lookups. */
	private static final SynonymLookup CHROM_LOOKUP = new SynonymLookup();
	
	/** Hash to map every synonym to all equivalent synonyms. */
	private final LinkedHashMap<String, Set<String>> lookupHash = new LinkedHashMap<String, Set<String>>();

	private final Set<String> preferredNames = new HashSet<String>();

	private Map<String, String> _lookupFastalinesHashMap = new HashMap<String, String>();

	/**
	 * Returns the default instance of SynonymLookup.  This is used to share
	 * a common SynonymLookup across the entire code.
	 *
	 * @return the default instance of SynonymLookup.
	 */
	public static SynonymLookup getDefaultLookup() {
		return DEFAULT_LOOKUP;
	}

	/**
	 * Returns the default instance of SynonymLookup.  This is used to share
	 * a common SynonymLookup across the entire code.
	 *
	 * @return the default instance of chromosome SynonymLookup.
	 */
	public static SynonymLookup getChromosomeLookup(){
		return CHROM_LOOKUP;
	}
	/**
	 * Loads synonyms from the given input stream.
	 *
	 * @param istream the input stream to load synonyms from.
	 * @throws java.io.IOException if the input stream is null or an error occurs reading it.
	 */
	public void loadSynonyms(InputStream istream) throws IOException {
		this.loadSynonyms(istream, false);
	}

	public void loadSynonyms(InputStream istream, boolean setPreferredNames) throws IOException {
		InputStreamReader ireader = null;
		BufferedReader br = null;
		String line;
		
		try {
			ireader = new InputStreamReader(istream);
			br = new BufferedReader(ireader);
			while ((line = br.readLine()) != null) {
				//Ignore comments.
				if(line.startsWith("#"))
					continue;

				String[] fields = LINE_REGEX.split(line);
				if (fields.length >= 2) {
					if (setPreferredNames) {
						preferredNames.add(fields[0]);
					}
					addSynonyms(fields);
				}
			}
		} finally {
			GeneralUtils.safeClose(ireader);
			GeneralUtils.safeClose(br);
		}
	}

	/**
	 * Add synonyms to this synonym lookup.
	 * <p />
	 * The input array of synonyms may contain null or empty strings, they will
	 * be filtered out during processing.
	 *
	 * @param syns the string array of synonyms to add to this synonym lookup.
	 */
	public synchronized void addSynonyms(String[] syns) {
		Set<String> synonymList = new LinkedHashSet<String>(Arrays.asList(syns));
		Set<String> previousSynonymList;

		for (String newSynonym : syns) {
			if (newSynonym == null) {
				continue;
			}
			newSynonym = newSynonym.trim();
			if (newSynonym.length() == 0) {
				continue;
			}
			previousSynonymList = lookupHash.put(newSynonym, synonymList);

			if (previousSynonymList != null) {
				for (String existingSynonym : previousSynonymList) {
					if (synonymList.add(existingSynonym)) {
						// update lookupHash if existing synonym not
						// already in synonym list.
						lookupHash.put(existingSynonym, synonymList);
					}
				}
			}
		}
	}

	/**
	 * Return the Set of all known synonyms.
	 *
	 * @return Set of all known synonyms.
	 */
	public Set<String> getSynonyms() {
		return lookupHash.keySet();
	}
	
	/**
	 * Return all known synonyms for the given synonym.  Will return an empty
	 * list if the synonym is unknown.
	 * <p />
	 * Case sensitive lookup of the synonym is done using the default behaviour.
	 * The default case sensitive behaviour is {@value #DEFAULT_CS}.
	 *
	 * @param synonym the synonym to find matching synonyms for.
	 * @return the set of matching synonyms for the given synonym or an empty set.
	 * @see #DEFAULT_CS
	 */
	public Set<String> getSynonyms(String synonym) {
		return getSynonyms(synonym, DEFAULT_CS);
	}

	/**
	 * Return all known synonyms for the given synonym.  Will return an empty
	 * list if the synonym is unknown.
	 * <p />
	 * The lookup of the synonym will be case sensitive if cs is true.
	 *
	 * @param synonym the synonym to find the matching synonyms for.
	 * @param cs set the case-sensitive behaviour of the synonym lookup.
	 * @return the set of matching synonyms for the given synonym or an epmty set.
	 */
	public Set<String> getSynonyms(String synonym, boolean cs) {
		if (synonym == null) {
			throw new IllegalArgumentException("str can not be null");
		}

		if (cs) {
            if (lookupHash.containsKey(synonym)) {
				return Collections.<String>unmodifiableSet(lookupHash.get(synonym));
			} else {
				return Collections.<String>emptySet();
			}
        } else {
			Set<String> synonyms = new LinkedHashSet<String>();

            for (String key : lookupHash.keySet()) {
                if (key.equalsIgnoreCase(synonym)) {
                    synonyms.addAll(lookupHash.get(key));
                }
            }
			return Collections.<String>unmodifiableSet(synonyms);
        }
	}

	/**
	 * Determine if two potential synonyms are synonymous using the default
	 * lookup rules.
	 * <p />
	 * The default behaviour of case sensitivity is {@value #DEFAULT_CS}.
	 * <p />
	 * The default behaviour of strip random is {$value #DEFAULT_SR}.
	 *
	 * @param synonym1 the first potential synonym.
	 * @param synonym2 the second potential synonym.
	 * @return true if the two parameters are synonymous.
	 * @see #DEFAULT_CS
	 * @see #DEFAULT_SR
	 */
	public boolean isSynonym(String synonym1, String synonym2) {
		return isSynonym(synonym1, synonym2, DEFAULT_CS, DEFAULT_SR);
	}

	/**
	 * Determine if two potential synonyms are synonymous.
	 * <p />
	 * The cs parameter specifies if the synonym comparison is case sensitive.
	 * True if the comparison should be case sensitive, false otherwise.
	 * <p />
	 * The sr parameter specifies if the synonym comparison should strip
	 * '_random' from the synonyms if the initial comparison is false.  True
	 * if random should be stripped from the potential synonyms.
	 *
	 * @param synonym1 the first potential synonym.
	 * @param synonym2 the second potential synonym.
	 * @param cs the case sensitivity of this query.
	 * @param sr whether tailing '_random' of the synonyms should be stripped before comparison.
	 * @return true or false
	 */

	public boolean isSynonym(String synonym1, String synonym2, boolean cs, boolean sr) {
		if (synonym1 == null || synonym2 == null) {
			throw new IllegalArgumentException("synonyms can not be null");
		}

		Collection<String> synonyms = getSynonyms(synonym1, cs);

		if (sr && hasRandom(synonym1, cs) && hasRandom(synonym2, cs)) {
			return isSynonym(stripRandom(synonym1), stripRandom(synonym2), cs, sr);
		} else if (cs) {
			return synonyms.contains(synonym2);
		} else {
			for (String curstr : synonyms) {
				if (synonym2.equalsIgnoreCase(curstr)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Find the preferred name of the given synonym.  Under the hood, this just
	 * returns the first synonym in the list of available synonyms for the
	 * input.
	 * <p />
	 * Will return the input synonym if no synonyms are known.
	 * <p />
	 * Case sensitive lookup of the synonym is done using the default behaviour.
	 * The default case sensitive behaviour is {@value #DEFAULT_CS}.
	 *
	 * @param synonym the synonym to find the preferred name of.
	 * @return the preferred name of the synonym.
	 */
	public String getPreferredName(String synonym) {
		return getPreferredName(synonym, DEFAULT_CS);
	}

	/**
	 * Find the preferred name of the given synonym.  Under the hood, this just
	 * returns the first synonym in the list of available synonyms for the
	 * input.
	 * <p />
	 * Will return the input synonym if no synonyms are known.
	 * <p />
	 * The lookup of the synonym will be case sensitive if cs is true.
	 *
	 * @param synonym the synonym to find the preferred name of.
	 * @param cs set the case-sensitive behaviour of the synonym lookup.
	 * @return the preferred name of the synonym.
	 */
	public String getPreferredName(String synonym, boolean cs) {
		return this.findMatchingSynonym(preferredNames, synonym, cs, false);
	}

	/**
	 * Finds the first synonym in a list that matches the given synonym.
	 * <p />
	 * Case sensitive lookup of the synonym is done using the default behaviour.
	 * The default case sensitive behaviour is {@value #DEFAULT_CS}.
	 * <p />
	 * Stripping '_random' from the synonym is done using the default behaviour.
	 * The default strip random behaviour is {@value #DEFAULT_SR}
	 *
	 * @param choices a list of possible synonyms that might match the given synonym.
	 * @param synonym  the id you want to find a synonym for.
	 * @return either null or a String synonym, where isSynonym(synonym, synonym) is true.
	 * @see #DEFAULT_CS
	 * @see #DEFAULT_SR
	 */
	public String findMatchingSynonym(Collection<String> choices, String synonym) {
		return findMatchingSynonym(choices, synonym, DEFAULT_CS, DEFAULT_SR);
	}

	/**
	 * Finds the first synonym in a list that matches the given synonym.
	 * <p />
	 * The lookup of the synonym will be case sensitive if cs is true.
	 * <p />
	 * the lookup will strip '_random' from the synonyms if sr is true.
	 *
	 * @param choices a list of possible synonyms that might match the given synonym.
	 * @param synonym  the id you want to find a synonym for.
	 * @param cs set the case-sensitive behaviour of the synonym lookup.
	 * @param sr set the strip random behaviour of the synonym lookup.
	 * @return either String synonym, where isSynonym(synonym, synonym) is true or the original synonym.
	 */
	public String findMatchingSynonym(Collection<String> choices, String synonym, boolean cs, boolean sr) {
		for (String id : choices) {
			if (this.isSynonym(synonym, id, cs, sr)) {
				return id;
			}
		}
		return synonym;
	}

	/**
	 * Find the second synonym, if it exists.  Otherwise return the first.
	 * @param synonym
	 * @return
	 */
	public String findSecondSynonym(String synonym) {
		Set<String> synonymSet = this.lookupHash.get(synonym);
		if (synonymSet == null) {
			return synonym;
		}
		String firstSynonym = "";
		for (String id : this.lookupHash.get(synonym)) {
			if (firstSynonym.length() == 0) {
				firstSynonym = id;
			} else {
				return id;	// second synonym
			}
		}
		return firstSynonym;
	}

	/**
	 * Determine if a synonym ends with '_random'.  Detection will be case
	 * sensitive if cs is true.
	 *
	 * @param synonym the synonym to test for ending with '_random'.
	 * @param cs the case sensitivity of the comparison.
	 * @return true if the synonym ends with '_random'.
	 */
	private static boolean hasRandom(String synonym, boolean cs) {
		if (synonym == null) {
			throw new IllegalArgumentException("synonym can not be null");
		} else if (!cs && synonym.toLowerCase().endsWith("_random")) {
			return true;
		} else if (synonym.endsWith("_random")) {
			/* effectively case-sensitive check */
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Strip the string '_random' from the end of the synonym.  It is up to the
	 * caller to ensure that the synonym actually ends with '_random'.
	 *
	 * @param synonym the synonym to strip '_random' from.
	 * @return the synonym sans the '_random'.
	 */
	private static String stripRandom(String synonym) {
		if (!synonym.toLowerCase().endsWith("_random")) {
			throw new IllegalArgumentException("synonym must end with '_random'");
		}
		return synonym.substring(0, synonym.length() - 7);
	}


	/**
	 * Function to load metatie fastalines to map RefSeq to genome name
	 * 
	 * @param filePath the path to metatie_fastalines file
	 * @return true if the file is correctly loaded otherwise false
	 */
	public boolean loadMetatieFastalines(String filePath) {
		try {
			FileReader fileReader = new FileReader(filePath);
			BufferedReader bufReader = new BufferedReader(fileReader);
			String currentLine;

			while ((currentLine = bufReader.readLine()) != null) {

				//Get necessary informations from the current line (RefSeq id and genome name)
				String workingString = currentLine.substring(currentLine.indexOf("|NC_") + 1);

				//safe the refseq index and the corresponding chromesome name
				String refSeq = workingString.substring(0, workingString.indexOf("|"));
				String genomeName = workingString.substring(workingString.indexOf("|") + 1, workingString.length());
				_lookupFastalinesHashMap.put(refSeq, genomeName);
			}
		}
		catch (FileNotFoundException ffe) {
			System.out.println(ffe.getMessage());
			return false;
		}
		catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Get the genome name to a given RefSeq id
	 * @param refSeq the RefSeq id to look for
	 * @return the corresponding genome name
	 */
	public String getGenomeNameFromRefSeq(String refSeq){

		if(_lookupFastalinesHashMap.isEmpty())
			return "no lookup table set";

		String genomeName;
		
		if((genomeName = _lookupFastalinesHashMap.get(refSeq)) == null)
			return "unknown genome";
		else
			return genomeName;
	}
}
