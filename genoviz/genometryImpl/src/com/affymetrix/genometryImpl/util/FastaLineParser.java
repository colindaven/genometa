package com.affymetrix.genometryImpl.util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FastaLineParser {
	
	/**
	 * Version 0.31
	 * Author : Colin Davenport, Hanover Medical School
	 * 
	 * Changelog
	 * 0.31 added NCIB, NRRL recognition as first part of strain name
	 * 0.32 refactoring, optimized array filling and javadoc
	 */
	
	private String _fastaHeader = "";
	private String _fastaHeaderTextPart = "";

	private String _genus = "";
	private String _species = "";
	private String _strain = "";
	private String _refSeq = "";
	private String _name = "";
	private List<String> _wordList = null;
	private boolean _printStarting = true;
	
	private int _fakeRefSeqCounter = 0;

	/**
	 * Method to inwoke parsinf a fastaline
	 * @param fastaHead fasta line to parse
	 */
	public void parseFastaHeader(String fastaHead) {
		
		if (_printStarting) {
			System.out.println("Attempting to parse fasta lines");
			_printStarting = false;
		}
		
		
		_fastaHeader = fastaHead;
	
		extractNecessaryParts();
		
		if (_wordList.size() >= 3) {
			
			_refSeq = getNCNumber();
			_genus = parseGenus();
			_species = parseSpecies();
			_strain = parseStrain();			
		}

		else {
			_refSeq = getNCNumber(); //will make fake NC_ number
			_genus = "unknown";
			_species = "sp";
			_strain = "str";
		}

	}

	/**
	 * Method to extract the necessary parts out of of the whole fasta line
	 */
	private void extractNecessaryParts() {
		
		String fastaHeaderTextPart = "";
		String[] splitWords = null;
		_wordList = new ArrayList<String>();
		
		if (_fastaHeader.contains("| ")) {
			int i = 0;
			for (i = 0; i< _fastaHeader.length()-2 ; i++) {
				if (_fastaHeader.substring(i, i+2).contains("| ")) {
					break;
				}
			}

			i = i + 2;
			fastaHeaderTextPart = _fastaHeader.substring(i, _fastaHeader.length());

			splitWords = fastaHeaderTextPart.split(" ");

			
			/*for (int q=0; q<splitWords.length; q++) {
				_wordList.add(splitWords[q]);
			}*/
			
			_wordList.addAll(Arrays.asList(splitWords));
		}

		else if (_fastaHeader.contains("|BBUR")) {
			int i = 0;
			for (i = 0; i< _fastaHeader.length()-2 ; i++) {

				if (_fastaHeader.substring(i, i+2).contains("|B")) {
					break;
				}
			}
			i = i + 5;
			fastaHeaderTextPart = _fastaHeader.substring(i, _fastaHeader.length());
			splitWords = fastaHeaderTextPart.split(" ");
			
			/*for (int q = 0; q < splitWords.length; q++) {
				_wordList.add(splitWords[q]);
			}*/
			
			_wordList.addAll(Arrays.asList(splitWords));
		}
		else {
			System.out.println("No '| ' found to divide accessions and genus / species / strain names " + _fastaHeader);
		}
	}

	/**
	 * Method to parse out the genus out of the fastaline
	 * @return the parsed genus
	 */
	private String parseGenus() {
		String tmpGenus = "";
		
		if (_wordList.get(0).equalsIgnoreCase("Candidatus")) {
			tmpGenus = _wordList.get(1);
		}
		else {
			tmpGenus = _wordList.get(0);
		}
		
		return tmpGenus;
	}

	/**
	 * Method to parse out the species out of the fastaline
	 * @return the parsed species
	 */
	private String parseSpecies() {
		String tmpSpecies = "";
		
		if (!_wordList.get(1).contains("sp.")) {
			tmpSpecies = _wordList.get(1);
		}
		else {
			tmpSpecies = "sp";
		}
		if (tmpSpecies.contains(",")) {
			tmpSpecies = tmpSpecies.replace(',', ' ');
		}
		return tmpSpecies;
	}

	/**
	 * Method to parse out the strain out of the fastaline
	 * @return the parsed strain
	 */
	private String parseStrain() {
		
		String tmpStrain = "";

		for (String loopString: _wordList) {
			if (loopString.contains("str.")) {
				//check in loop
				
				for (int i=0;i<_wordList.size();i++) {
					if (_wordList.get(i).contains("str.")) {
						tmpStrain = _wordList.get(i+1);
						if (	tmpStrain.contains("ATCC") ||
								tmpStrain.contains("CFN") ||
								tmpStrain.contains("DSM") ||
								tmpStrain.contains("IP") ||
								tmpStrain.contains("IAM") ||
								tmpStrain.contains("PCC") ||
								tmpStrain.contains("RSA") ||
								tmpStrain.contains("FA") ||
								tmpStrain.contains("NCTC") ||
								tmpStrain.contains("IFM") ||
								tmpStrain.contains("MIT") ||
								tmpStrain.contains("USDA") ||
								tmpStrain.contains("AU") ||
								tmpStrain.contains("WH") ||
								tmpStrain.contains("ORS") ||
								tmpStrain.contains("OSU") ||
								tmpStrain.contains("APEC") ||
								tmpStrain.contains("DK") ||
								tmpStrain.contains("KCTC") ||
								tmpStrain.contains("SH") ||
								tmpStrain.contains("CIP") ||
								tmpStrain.contains("PAl") ||
								tmpStrain.contains("DFL") ||
								tmpStrain.contains("NCIB") ||
								tmpStrain.contains("NRRL") ||
								tmpStrain.contains("DPC")
								) {
							tmpStrain = tmpStrain + _wordList.get(i+2);
						}
						break;
					}			
				}

			}
			else if (loopString.contains("subsp.") && !loopString.contains("str.")) {
				for (int i=0;i<_wordList.size();i++) {
					if (_wordList.get(i).contains("subsp.")) {		//contains subsp. but not str.
						tmpStrain = _wordList.get(i+2);

						break;
					}			
				}
			}
			
		}
		if (tmpStrain.equalsIgnoreCase("")) {
			tmpStrain = _wordList.get(2);
			if (	tmpStrain.contains("ATCC") ||
					tmpStrain.contains("CFN") ||
					tmpStrain.contains("DSM") ||
					tmpStrain.contains("IP") ||
					tmpStrain.contains("IAM") ||
					tmpStrain.contains("PCC") ||
					tmpStrain.contains("RSA") ||
					tmpStrain.contains("FA") ||
					tmpStrain.contains("NCTC") ||
					tmpStrain.contains("IFM") ||
					tmpStrain.contains("MIT") ||
					tmpStrain.contains("USDA") ||
					tmpStrain.contains("AU") ||
					tmpStrain.contains("WH") ||
					tmpStrain.contains("ORS") ||
					tmpStrain.contains("OSU") ||
					tmpStrain.contains("APEC") ||
					tmpStrain.contains("DK") ||
					tmpStrain.contains("KCTC") ||
					tmpStrain.contains("SH") ||
					tmpStrain.contains("CIP") ||
					tmpStrain.contains("PAl") ||
					tmpStrain.contains("DFL") ||
					tmpStrain.contains("NCIB") ||
					tmpStrain.contains("NRRL") ||
					tmpStrain.contains("DPC")
					) {
						tmpStrain = tmpStrain + _wordList.get(3);
			}
		}
		if (tmpStrain.contains("serovar") || tmpStrain.contains("sv.")
					|| tmpStrain.contains("pathovar") || tmpStrain.contains("pv.")
					|| tmpStrain.contains("biovar") || tmpStrain.contains("bv.")) {
			for (int i=0;i<_wordList.size();i++) {
				if (_wordList.get(i).contains("serovar") || _wordList.get(i).contains("sv.") 
							|| _wordList.get(i).contains("pathovar") || _wordList.get(i).contains("pv.")
							|| _wordList.get(i).contains("biovar") || _wordList.get(i).contains("bv.")) {
					
					tmpStrain = _wordList.get(i+2);

					break;
				}			
			}
		}
		// If after all this string STILL is only equal to one part of the 2 part ATCC XX 
		// i.e. if the strain information is 2 part still
		if (	tmpStrain.equals("ATCC") ||
				tmpStrain.equals("CFN") ||
				tmpStrain.equals("DSM") ||
				tmpStrain.equals("IP") ||
				tmpStrain.equals("IAM") ||
				tmpStrain.equals("PCC") ||
				tmpStrain.equals("RSA") ||
				tmpStrain.equals("FA") ||
				tmpStrain.equals("NCTC") ||
				tmpStrain.equals("IFM") ||
				tmpStrain.equals("MIT") ||
				tmpStrain.equals("USDA") ||
				tmpStrain.equals("AU") ||
				tmpStrain.equals("WH") ||
				tmpStrain.equals("ORS") ||
				tmpStrain.equals("OSU") ||
				tmpStrain.equals("APEC") ||
				tmpStrain.equals("DK") ||
				tmpStrain.equals("KCTC") ||
				tmpStrain.equals("SH") ||
				tmpStrain.equals("CIP") ||
				tmpStrain.equals("PAl") ||
				tmpStrain.equals("DFL") ||
				tmpStrain.equals("NCIB") ||
				tmpStrain.equals("NRRL") ||
				tmpStrain.equals("DPC")
				) {
					//if (str.equals("ATCC") || str.equals("CFN") || str.equals("DSM") || str.equals("IP") || str.equals("IAM") || str.equals("PCC") || str.equals("RSA") || str.equals("FA") || str.equals("NCTC") || str.equals("IFM") || str.equals("MIT")|| str.contains("USDA") || str.contains("AU") || str.contains("WH")) {
					for (int i=0;i<_wordList.size();i++) {
						if (	_wordList.get(i).contains("ATCC") ||
								_wordList.get(i).contains("CFN") ||
								_wordList.get(i).contains("DSM") ||
								_wordList.get(i).contains("IP") ||
								_wordList.get(i).contains("IAM") ||
								_wordList.get(i).contains("PCC") ||
								_wordList.get(i).contains("RSA") ||
								_wordList.get(i).contains("FA") ||
								_wordList.get(i).contains("NCTC") ||
								_wordList.get(i).contains("IFM") ||
								_wordList.get(i).contains("MIT") ||
								_wordList.get(i).contains("USDA") ||
								_wordList.get(i).contains("AU") ||
								_wordList.get(i).contains("WH") ||
								_wordList.get(i).contains("ORS") ||
								_wordList.get(i).contains("OSU") ||
								_wordList.get(i).contains("APEC") ||
								_wordList.get(i).contains("DK") ||
								_wordList.get(i).contains("KCTC") ||
								_wordList.get(i).contains("SH") ||
								_wordList.get(i).contains("CIP") ||
								_wordList.get(i).contains("PAl") ||
								_wordList.get(i).contains("DFL") ||
								_wordList.get(i).contains("NCIB") ||
								_wordList.get(i).contains("NRRL") ||
								_wordList.get(i).contains("DPC")
								) {
						//if (wordList.get(i).contains("ATCC") || wordList.get(i).contains("CFN") || wordList.get(i).contains("DSM") || wordList.get(i).contains("IP") || wordList.get(i).contains("IAM") || wordList.get(i).contains("PCC") || wordList.get(i).contains("RSA")|| wordList.get(i).contains("FA")|| wordList.get(i).contains("NCTC")|| wordList.get(i).contains("MIT") || wordList.get(i).contains("USDA") || wordList.get(i).contains("AU") || wordList.get(i).contains("WH")) {		
									tmpStrain = tmpStrain + _wordList.get(i+1);
									break;
								}			
					}
				}
		if (tmpStrain.contains(",")) {
			tmpStrain = tmpStrain.replace(',', ' ');
		}
		if (tmpStrain.contains(".")) {
			tmpStrain = tmpStrain.replace('.', ' ');
		}
		if (tmpStrain.contains("'")) {
			tmpStrain = tmpStrain.replace('\'', ' ');
		}
		if (tmpStrain.contains("complete")) {
			tmpStrain = tmpStrain.replace("complete", " ");
		}
		if (tmpStrain.contains("/")) {
			tmpStrain = tmpStrain.replace('/', ' ');
		}

		return tmpStrain;
	}

	/**
	 * Returns the parsed refseq id
	 * @return the parsed redfseq id
	 */
	public String getNCNumber() {
		
		String NCNumber = "";
		
		if (_fastaHeader.contains("NC_")) {
			for (int i = 0; i< _fastaHeader.length()-3 ; i++) {
				if (_fastaHeader.substring(i, i+3).contains("NC_")) {
					NCNumber = _fastaHeader.substring(i, i+9);
				}
			}	
		}
		else {
			System.out.println("Could not find NC number, generating a fake one "+_fastaHeader);
			_fakeRefSeqCounter++;
			if (_fakeRefSeqCounter < 10) {
				NCNumber = "NC_10000"+(String.valueOf(_fakeRefSeqCounter));
			}
			else if (_fakeRefSeqCounter >= 10 && _fakeRefSeqCounter < 100) {
				NCNumber = "NC_1000"+(String.valueOf(_fakeRefSeqCounter));
			}
			else if (_fakeRefSeqCounter >= 100 && _fakeRefSeqCounter < 1000) {
				NCNumber = "NC_100"+(String.valueOf(_fakeRefSeqCounter));
			}
			else {
				System.out.println("Unable to generate a fake RefSeq ");
			}	
//			fakeRefSeqCounter++;	//now set by method
		}	
		return NCNumber;
	}
	
	/**
	 * Returns the parsed genus name (refseq + genus + species)
	 * @return the genus name
	 */
	public String getName() {
		_name = _refSeq + _genus +"_" + _species;
		if (_name.contains(".")) {
			_name = _name.replace(',', ' ');
		}
		if (_name.contains(",")) {
			_name = _name.replace(',', ' ');
		}
		return _name;
	}

	/**
	 * Returns the parsed refseq id
	 * @return the refseq id
	 */
	public String getRefSeq() {
		return _refSeq;
	}

	/**
	 * Returns the parsed genus name
	 * @return the genus name
	 */
	public String getGenus() {
		return _genus;
	}

	/**
	 * Returns the parsed species
	 * @return the species name
	 */
	public String getSpecies() {
		return _species;
	}

	/**
	 * Retruns the parses strain
	 * @return the name of the strain
	 */
	public String getStrain() {
		return _strain;
	}

	/**
	 *
	 * @param runCounter
	 */
	private void setRunCounter(Integer runCounter) {
		//for the fake fastalineheader
		_fakeRefSeqCounter = runCounter;
		
	}

}
