package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.symloader.SAM.FeatureIndex.ChromosomeIndex;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.SeekableStreamFactory;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.SamUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMFormatException;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.CloseableIterator;
import net.sf.samtools.util.SeekableStream;


public class SAM extends SymLoader {

	private static List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("sam");
	}

	static final Logger log = Logger.getLogger("SAM");
    String samFile;
    SeekableStream stream;
    FeatureIndex featureIndex;
    private static final boolean DEBUG = false;
	protected SAMFileReader reader;
    protected SAMFileHeader header;
	protected final Set<BioSeq> seqs = new HashSet<BioSeq>();
	private File indexFile = null;

	private static List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		// SAM files are generally large, so only allow loading visible data.
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
	}

	public SAM(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri, featureName, seq_group);
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}


	private boolean initTheSeqs() {
		try {
			header = reader.getFileHeader();
			if (header == null || header.getSequenceDictionary() == null || header.getSequenceDictionary().getSequences() == null) {
				Logger.getLogger(SAM.class.getName()).log(Level.WARNING, "Couldn't find sequences in file");
				return false;
			}
			Thread thread = Thread.currentThread();
			for (SAMSequenceRecord ssr : header.getSequenceDictionary().getSequences()) {
				try {
					if (thread.isInterrupted()) {
						break;
					}
					String seqID = ssr.getSequenceName();
					BioSeq seq = group.getSeq(seqID);
					if (seq == null) {
						int seqLength = ssr.getSequenceLength();
						seq = new BioSeq(seqID, group.getID(), seqLength);
						Logger.getLogger(BAM.class.getName()).log(
								Level.FINE, "Adding chromosome {0} to group {1}", new Object[]{seqID, group.getID()});
						group.addSeq(seq);
					}
					seqs.add(seq);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			return !thread.isInterrupted();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public List<BioSeq> getChromosomeList() {
		init();
		return new ArrayList<BioSeq>(seqs);
	}

	@Override
	public List<SeqSymmetry> getGenome() {
		init();
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		for (BioSeq seq : group.getSeqList()) {
			results.addAll(getChromosome(seq));
		}
		return results;
	}

	@Override
	public List<SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		return parse(seq, seq.getMin(), seq.getMax(), true, false);
	}


	@Override
	public List<SeqSymmetry> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(), span.getMin(), span.getMax(), true, false);
	}


	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		try{
			this.samFile = uri.toString();
			stream = SeekableStreamFactory.getStreamFor(uri);
			String scheme = uri.getScheme().toLowerCase();
			if (scheme.length() == 0 || scheme.equals("file")) {
				// SAM is file.
				File f = new File(uri);
				reader = new SAMFileReader(f);
				reader.setValidationStringency(ValidationStringency.SILENT);
				featureIndex = getIndexFor(samFile);
			}else if (scheme.startsWith("http")) {
				// BAM is URL.  Get the indexed .bai file, and query only the needed portion of the BAM file.

				String uriStr = uri.toString();
				// Guess at the location of the .bai URL as BAM URL + ".bai"
				String saiUriStr = uriStr + ".sai";
				indexFile = LocalUrlCacher.convertURIToFile(URI.create(saiUriStr));
				if (indexFile == null) {
					ErrorHandler.errorPanel("No SAM index file",
							"Could not find URL of SAM index at " + saiUriStr + ". "
							+ "Please be sure this is in the same directory as the SAM file.");
					this.isInitialized = false;
					return;
				}
				reader = new SAMFileReader(uri.toURL(), indexFile, false);
				reader.setValidationStringency(ValidationStringency.SILENT);
				featureIndex = getIndexFor(uriStr);
			} else {
				Logger.getLogger(SAM.class.getName()).log(
						Level.SEVERE, "URL scheme: {0} not recognized", scheme);
				return;
			}

			if(featureIndex == null)
				return;
			
			if(initTheSeqs()){
				super.init();
			}

		}catch(SAMFormatException ex){
			ErrorHandler.errorPanel("SAM exception", "A SAMFormatException has been thrown by the Picard tools.\n" +
					"Please validate your SAM files and contact the Picard project at http://picard.sourceforge.net." +
					"See console for the details of the exception.\n");
			ex.printStackTrace();
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}


	public List<SeqSymmetry> parse(BioSeq seq, int min, int max, boolean containerSym, boolean contained) {
		init();
		List<SeqSymmetry> symList = new ArrayList<SeqSymmetry>(1000);
		CloseableIterator<SAMRecord> iter = null;
		try {
			if (reader != null) {
				iter = query(seq.getID(), min, max, contained);
				if (iter != null && iter.hasNext()) {
					SAMRecord sr = null;
					while(iter.hasNext() && (!Thread.currentThread().isInterrupted())){
						sr = iter.next();
						symList.add(SamUtils.convertSAMRecordToSymWithProps(sr, seq, featureName, featureName));
					}
				}
			}
		} finally {
			if (iter != null) {
				iter.close();
			}
		}
		return symList;
	}


    public synchronized CloseableIterator<SAMRecord> query(final String sequence, final int start, final int end, final boolean contained) {

        if (featureIndex == null) {
            featureIndex = getIndexFor(samFile);
        }

        if (featureIndex == null) {
            throw new java.lang.UnsupportedOperationException("SAM files must be indexed to support query methods");
        }
        if (!featureIndex.containsChromosome(sequence)) {
            return null;
        }

        // If contained == false (include overlaps) we need to adjust the start to
        // ensure we get features that extend into this segment.
        int startAdjustment = contained ? 0 : featureIndex.getLongestFeature(sequence);
        int startTileNumber = Math.max(0, (start - startAdjustment)) / featureIndex.getTileWidth();

        FeatureIndex.TileDef seekPos = featureIndex.getTileDef(sequence, startTileNumber);

        if (seekPos != null) {

            try {
                // Skip to the start of the chromosome (approximate)
                stream.close();
                stream = SeekableStreamFactory.getStreamFor(uri);
                stream.seek(seekPos.getStartPosition());
                SAMFileReader sfreader = new SAMFileReader(stream);
                sfreader.setValidationStringency(ValidationStringency.SILENT);

                CloseableIterator<SAMRecord> iter = sfreader.iterator();
                return new SAMQueryIterator(sequence, start, end, contained, iter);

            } catch (IOException ex) {
                log.log(Level.SEVERE,"Error opening sam file", ex);
            }
        }
        return null;
    }

    public boolean hasIndex() {
        if (featureIndex == null) {
            getIndex();
        }
        return featureIndex != null;
    }

    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }


    private FeatureIndex getIndex() {
        if (featureIndex == null) {
            featureIndex = getIndexFor(samFile);
        }
        return featureIndex;
    }

    public Set<String> getSequenceNames() {
        FeatureIndex idx = getIndex();
        if (idx == null) {
            return null;
        } else {
            return idx.getIndexedChromosomes();
        }

    }

    public CloseableIterator<SAMRecord> iterator() {
        SAMFileReader sfreader = new SAMFileReader(stream);
        sfreader.setValidationStringency(ValidationStringency.SILENT);
        CloseableIterator<SAMRecord> iter = sfreader.iterator();
        return new SAMQueryIterator(iter);

    }

    /**
     *
     */
    class SAMQueryIterator implements CloseableIterator<SAMRecord> {

        String chr;
        int start;
        int end;
        boolean contained;
        SAMRecord currentRecord;
        CloseableIterator<SAMRecord> wrappedIterator;

        public SAMQueryIterator(CloseableIterator<SAMRecord> wrappedIterator) {
            this.chr = null;
            this.wrappedIterator = wrappedIterator;
            currentRecord = wrappedIterator.next();
        }

        public SAMQueryIterator(String sequence, int start, int end, boolean contained,
                                CloseableIterator<SAMRecord> wrappedIterator) {
            this.chr = sequence;
            this.start = start;
            this.end = end;
            this.contained = contained;
            this.wrappedIterator = wrappedIterator;
            advanceToFirstRecord();
        }

        private void advanceToFirstRecord() {
            while (wrappedIterator.hasNext()) {
                currentRecord = wrappedIterator.next();
                if (!currentRecord.getReferenceName().equals(chr)) {
                    break;
                } else if ((contained && currentRecord.getAlignmentStart() >= start) ||
                        (!contained && currentRecord.getAlignmentEnd() >= start)) {
                    break;
                }
            }
        }

        public void close() {
            wrappedIterator.close();
        }

        public boolean hasNext() {
            if (chr == null && currentRecord != null) {
                return true;
            }
            if (currentRecord == null || (chr != null && !chr.equals(currentRecord.getReferenceName()))) {
                return false;
            } else {
                return contained ? currentRecord.getAlignmentEnd() <= end
                        : currentRecord.getAlignmentStart() <= end;
            }
        }

        public SAMRecord next() {
            SAMRecord ret = currentRecord;
            if (wrappedIterator.hasNext()) {
                currentRecord = wrappedIterator.next();
            } else {
                currentRecord = null;
            }
            return ret;

        }

        public void remove() {
            //throw new UnsupportedOperationException("Not supported yet.");
        }
    }
	
	public static FeatureIndex getIndexFor(String samPath) {
        String idxPath = samPath + ".sai";
        return new FeatureIndex(idxPath);
    }

	public static class FeatureIndex {

		private int tileWidth;
		private Map<String, ChromosomeIndex> chrIndeces;
		private static final Logger log = Logger.getLogger("FeatureIndex");

		/**
		 * Constructs ...
		 *
		 * @param tileWidth
		 */
		public FeatureIndex(int tileWidth) {
			this.tileWidth = tileWidth;
			chrIndeces = new LinkedHashMap<String, ChromosomeIndex>();

		}

		/**
		 */
		public FeatureIndex(File f) {
			this(f.getAbsolutePath());
		}

		/**
		 */
		public FeatureIndex(String path) {

			InputStream is = null;
			try {
				is = LocalUrlCacher.getInputStream(path);
				chrIndeces = new LinkedHashMap<String, ChromosomeIndex>();
				read(is);
			} catch (IOException ex) {
				log.log(Level.SEVERE,"Error reading index", ex);
				//throw new DataLoadException("Error reading index: " + ex.getMessage(), path);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
					}
				}
			}

		}

		public boolean containsChromosome(String chr) {
			return chrIndeces.containsKey(chr);
		}

		public Set<String> getIndexedChromosomes() {
			return chrIndeces.keySet();
		}

		/**
		 * @param chr
		 * @param idx
		 * @param count
		 */
		public void add(String chr, long idx, int count, int longestFeature) {

			ChromosomeIndex chrIndex = chrIndeces.get(chr);
			if (chrIndex == null) {
				chrIndex = new ChromosomeIndex(longestFeature);
				chrIndeces.put(chr, chrIndex);
			}
			chrIndex.addTile(new TileDef(idx, count));

		}

		/**
		 * @param chr
		 * @param tile
		 * @return
		 */
		public TileDef getTileDef(String chr, int tile) {

			ChromosomeIndex chrIdx = chrIndeces.get(chr);
			if (chrIdx == null) {

				// Todo -- throw an execption ?
				return null;
			} else {
				return chrIdx.getTileDefinition(tile);
			}
		}

		/**
		 * Store a SamIndex to a stream.
		 * <p/>
		 * It is the responsibility of the caller  to close the stream.
		 *
		 * @param f
		 * @throws IOException
		 */
		public void store(File f) throws IOException {

			DataOutputStream dos = null;

			try {
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));

				dos.writeInt(getTileWidth());

				for (Map.Entry<String, ChromosomeIndex> entry : chrIndeces.entrySet()) {

					ChromosomeIndex chrIdx = entry.getValue();
					List<TileDef> tmp = chrIdx.getTileDefinitions();

					if (entry.getKey() != null) {
						dos.writeUTF(entry.getKey());
						dos.writeInt(tmp.size());
						dos.writeInt(chrIdx.getLongestFeature());

						for (int i = 0; i < tmp.size(); i++) {
							final FeatureIndex.TileDef tileDef = tmp.get(i);
							dos.writeLong(tileDef.getStartPosition());
							dos.writeInt(tileDef.getCount());
						}
					}
				}
			} finally {
				dos.close();
			}

		}

		/**
		 * Read the index.  Translate the chromosome names to the current genome aliases, if any.
		 *
		 * @throws IOException
		 */
		private void read(InputStream is) throws IOException {

			DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

			tileWidth = dis.readInt();
			try {
				while (true) {
					String chr = dis.readUTF();
					int nTiles = dis.readInt();
					int longestFeature = dis.readInt();

					List<TileDef> tileDefs = new ArrayList<TileDef>(nTiles);
					int tileNumber = 0;
					while (tileNumber < nTiles) {
						long pos = dis.readLong();
						int count = dis.readInt();
						tileDefs.add(new TileDef(pos, count));
						tileNumber++;
					}

					chrIndeces.put(chr, new ChromosomeIndex(longestFeature, tileDefs));
				}
			} catch (EOFException e) {
				// This is normal.  Unfortuantely we don't have a better way to test EOF for this stream
			} catch (Exception e) {
				log.log(Level.SEVERE, "Error reading chromosome name. ", e);
			}

		}

		public int getTileWidth() {
			return tileWidth;
		}

		public int getLongestFeature(String chr) {
			ChromosomeIndex tmp = this.chrIndeces.get(chr);
			if (tmp == null) {
				return 1000;
			} else {
				return tmp.getLongestFeature();
			}
		}

		static class ChromosomeIndex {

			private int longestFeature;
			private List<TileDef> tileDefinitions;

			ChromosomeIndex(int longestFeature) {
				this(longestFeature, new ArrayList<TileDef>());
			}

			public ChromosomeIndex(int longestFeature, List<TileDef> tileDefinitions) {
				this.longestFeature = longestFeature;
				this.tileDefinitions = tileDefinitions;
			}

			void addTile(TileDef tileDef) {
				getTileDefinitions().add(tileDef);
			}

			TileDef getTileDefinition(int i) {
				if (getTileDefinitions().isEmpty()) {
					// TODO -- throw exception ?
					return null;
				}
				int tileNumber = Math.min(i, getTileDefinitions().size() - 1);
				return getTileDefinitions().get(tileNumber);
			}

			/**
			 * @return the longestFeature
			 */
			public int getLongestFeature() {
				return longestFeature;
			}

			/**
			 * @return the tileDefinitions
			 */
			public List<TileDef> getTileDefinitions() {
				return tileDefinitions;
			}
		}

		public static class TileDef {

			private long startPosition;
			private int count;

			/**
			 * Constructs ...
			 *
			 * @param startPosition
			 * @param count
			 */
			public TileDef(long startPosition, int count) {
				this.startPosition = startPosition;
				this.count = count;
			}

			/**
			 * @return the startPosition
			 */
			public long getStartPosition() {
				return startPosition;
			}

			/**
			 * @return the count
			 */
			public int getCount() {
				return count;
			}
		}
	}
}
