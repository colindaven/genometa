package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hiralv
 * @version $Id$
 */
public final class TwoBitIterator implements SearchableCharIterator {

	/** Use a 4KB buffer, as that is the block size of most file systems */
	private static final int BUFFER_SIZE = 4096;
	
	/** Number of residues in each byte */
	private static final int RESIDUES_PER_BYTE = 4;

	/** Byte mask for translating unsigned bytes into Java integers */
    private static final int BYTE_MASK = 0xff;

	/** Character mask for translating binary into Java chars */
	private static final int CHAR_MASK = 0x03;

	private static final char[] BASES = { 'T', 'C', 'A', 'G'};

	private final URI uri;
	private final long length, offset;
	private final MutableSeqSymmetry nBlocks, maskBlocks;
	private final ByteOrder byteOrder;

	public TwoBitIterator(URI uri, long length, long offset, ByteOrder byteOrder, MutableSeqSymmetry nBlocks, MutableSeqSymmetry maskBlocks) {
		this.uri	    = uri;
		this.length     = length;
		this.offset     = offset;
		this.nBlocks    = nBlocks;
		this.maskBlocks = maskBlocks;
		this.byteOrder  = byteOrder;

		if (this.length > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("IGB can not handle sequences larger than " + Integer.MAX_VALUE + ".  Offending sequence length: " + length);
		}

	}

	/**
	 * Load data of size of buffer into buffer from file istr.
	 *
	 * @param istr	File istr from which data is to be loaded.
	 * @param buffer	Buffer in which data from file istr is read.
	 * @throws IOException
	 */
	private void loadBuffer(SeekableBufferedStream bistr, ByteBuffer buffer) throws IOException {
		buffer.rewind();
		bistr.read(buffer.array());
		buffer.rewind();
	}

	/**
	 * Reads file and returns residues starting from start and ending at end.
	 * @param start		Start position.
	 * @param end		End position.
	 * @return			Returns string of residues.
	 */
	public String substring(int start, int end) {
		SeekableBufferedStream bistr = null;
		File file = null;
		try {
			//Sanity Check
			start = Math.max(0, start);
			end = Math.max(end, start);
			end = Math.min(end, getLength());
			int requiredLength = end - start;
			long startOffset = start / RESIDUES_PER_BYTE;
			long bytesToRead = calculateBytesToRead(start, end);
			int beginLength = Math.min(RESIDUES_PER_BYTE - start % 4, requiredLength);
			int endLength = Math.min(end % RESIDUES_PER_BYTE, requiredLength);
			if (bytesToRead == 1) {
				if (start % RESIDUES_PER_BYTE == 0) {
					beginLength = 0;
				} else {
					endLength = 0;
				}
			}

			bistr = new SeekableBufferedStream(LocalUrlCacher.getSeekableStream(uri));

			bistr.position(this.offset + startOffset);
			
			return parse(bistr, start, bytesToRead, requiredLength, beginLength, endLength);

		} catch (FileNotFoundException ex) {
			Logger.getLogger(TwoBitIterator.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex){
			Logger.getLogger(TwoBitIterator.class.getName()).log(Level.SEVERE, null, ex);
		} finally{
			GeneralUtils.safeClose(bistr);
		}
		return "";
	}

	private String parse(SeekableBufferedStream bistr, int start, long bytesToRead, int requiredLength, int beginLength, int endLength) throws IOException {
		char[] residues = new char[requiredLength];
		byte[] valueBuffer = new byte[BUFFER_SIZE];
		int residueCounter = 0;
		long residuePosition = start;

		MutableSeqSymmetry tempNBlocks = GetBlocks(start, nBlocks);
		MutableSeqSymmetry tempMaskBlocks = GetBlocks(start, maskBlocks);
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		buffer.order(this.byteOrder);

		loadBuffer(bistr, buffer);

		//packedDNA
		SeqSpan nBlock = null;
		SeqSpan maskBlock = null;
		char[] temp = null;
		for (int i = 0; i < bytesToRead; i += BUFFER_SIZE) {
			buffer.get(valueBuffer);
			for (int k = 0; k < BUFFER_SIZE && residueCounter < requiredLength; k++) {
				temp = parseByte(valueBuffer[k], k, bytesToRead, start, requiredLength, beginLength, endLength);
				for (int j = 0; j < temp.length && residueCounter < requiredLength; j++) {
					nBlock = processResidue(residuePosition, temp, j, nBlock, tempNBlocks, false);
					maskBlock = processResidue(residuePosition, temp, j, maskBlock, tempMaskBlocks, true);
					residues[residueCounter++] = temp[j];
					residuePosition++;
				}
			}
			bistr.position(bistr.position() + BUFFER_SIZE);
			loadBuffer(bistr, buffer);
		}

		return new String(residues);
	}

	/**
	 * Determines number of bytes to read in order to read all residues between start and end.
	 * @param start		Start position of residue.
	 * @param end		End position of residue.
	 * @return			Returns number of bytes to be read.
	 */
	private static long calculateBytesToRead(long start, long end) {

		if(start/RESIDUES_PER_BYTE == end/RESIDUES_PER_BYTE)
			return 1;

		int endExtra = end % RESIDUES_PER_BYTE == 0 ? 0 : 1;
		long bytesToRead = (end/RESIDUES_PER_BYTE) - (start/RESIDUES_PER_BYTE) + endExtra;

		return bytesToRead;
	}

	/**
	 * Get required number of block from blocks based upon start position.
	 * @param start		Start position of residues.
	 * @param blocks	Blocks. (Either maskBlocks or nBlocks)
	 * @return			Returns required number of block.
	 */
	private static MutableSeqSymmetry GetBlocks(long start, MutableSeqSymmetry blocks){

		MutableSeqSymmetry tempBlocks =  new SimpleMutableSeqSymmetry();

		for(int i=0; i<blocks.getSpanCount(); i++){
			SeqSpan span = blocks.getSpan(i);
			if(start > span.getStart() && start >= span.getEnd()){
				continue;
			}
			tempBlocks.addSpan(span);

		}

		return tempBlocks;
	}

	/**
	 * Process the residue if it falls into maskBlocks or nBlocks.
	 * @param residuePosition	Actual residue position.
	 * @param temp				Temporary array in which residues are stored to processed.
	 * @param pos				Position of residue in temporary array.
	 * @param block				Current block.
	 * @param blocks			maskBlocks or nBlocks.
	 * @param isMask			Boolean variable to check if it maskBlocks or nBlocks. True for the maskBlocks and false for nBlocks.
	 * @return					Returns current block.
	 */
	private static SeqSpan processResidue(long residuePosition, char temp[], int pos, SeqSpan block, MutableSeqSymmetry blocks, boolean isMask){
		if (block == null) {
			block = GetNextBlock(blocks);
		}

		if (block != null) {
			if (residuePosition == block.getEnd()) {
				blocks.removeSpan( block);
				block = null;
			} else if (residuePosition >= block.getStart()) {
				if(isMask)
					temp[pos] = Character.toLowerCase(temp[pos]);
				else
					temp[pos] = 'N';
			}
		}
		return block;
	}

	/**
	 * Gets next block from blocks.
	 * @param Blocks	Blocks.
	 * @return			Returns next block if present else returns null.
	 */
	private static SeqSpan GetNextBlock(MutableSeqSymmetry Blocks){
		if(Blocks.getSpanCount() > 0) {
			return Blocks.getSpan(0);
		}
		return null;
	}

	/**
	 * Determines number of residues to be read from current byte.
	 * @param valueBuffer		Byte from which residues are to read.
	 * @param k					Byte position. Required for special cases. i.e. first or last byte.
	 * @param bytesToRead		Total number of bytes to read.
	 * @param start				Start position for requested residue.
	 * @param requiredLength	Required length of residue. Need when only one byte is to read.
	 * @param beginLength		Residue length of the first byte.
	 * @param endLength			Residue length of the last byte.
	 * @return					Returns the read residues.
	 */
	private char[] parseByte(byte valueBuffer, int k, long bytesToRead, int start, int requiredLength, int beginLength, int endLength) {
		char temp[] = null;

		if (bytesToRead == 1) {
			temp = parseByte(valueBuffer, start % RESIDUES_PER_BYTE, requiredLength);
		} else if (k == 0 && beginLength != 0) {
			temp = parseByte(valueBuffer, beginLength, true);
		} else if (k == bytesToRead - 1 && endLength != 0) {
			temp = parseByte(valueBuffer, endLength, false);
		} else {
			temp = parseByte(valueBuffer);
		}

		return temp;
	}

	/**
	 * Special case to read residues. i.e when it first or last byte.
	 * @param valueBuffer	Byte from which residues are to be read.
	 * @param size			Size of resides to be read.
	 * @param isFirst		Boolean to determine if residues are to be read from first or last position. True for the first and false for last.
	 * @return				Returns read residues.
	 */
	private static char[] parseByte(byte valueBuffer, int size, boolean isFirst){
		char temp[] = parseByte(valueBuffer);
		char newTemp[] = new char[size];

		int skip = isFirst ? (temp.length - size) : 0;

			for(int i=0; i<size; i++){
				newTemp[i] = temp[skip+i];
			}

		return newTemp;
	}

	/**
	 * Special case to read residues. i.e. when only one byte is to read.
	 * @param valueBuffer	Byte from which residues are to be read.
	 * @param position		Position from residues are to be read.
	 * @param length		Length of residues to be read.
	 * @return				Returns read residues.
	 */
	private static char[] parseByte(byte valueBuffer, int position, int length) {
		char temp[] = parseByte(valueBuffer);
		char newTemp[] = new char[length];

		for(int i=0; i<length; i++){
			newTemp[i] = temp[position+i];
		}

		return newTemp;
	}

	/**
	 * General case to read residues.
	 * @param valueBuffer	Byte from which residues are to be read.
	 * @return				Returns read residues. Returns character of size four.
	 */
	private static char[] parseByte(byte valueBuffer){
		char temp[] = new char[RESIDUES_PER_BYTE];
		int dna, value = valueBuffer & BYTE_MASK;

		for (int j = RESIDUES_PER_BYTE; j > 0; j--) {
			dna = value & CHAR_MASK;
			value = value >> 2;
			temp[j-1] = BASES[dna];
		}

		return temp;
	}
	
	public int indexOf(String needle, int offset) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Returns length of residues.
	 * @return	Returns length of residues.
	 */
	public int getLength() {
		return (int) length;
	}


}
