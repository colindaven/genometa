package com.affymetrix.genometryImpl.das2;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.util.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;

/**
 *  
 *  Das2SeqGroup extends AnnotatedSeqGroup to represent a group of sequences (usually a genome) that was 
 *     initially accessed from a DAS/2 server as a Das2VersionedSource
 *  ensures that group's AnnotatedBioSeqs are instantiated when they are needed
 */
public final class Das2SeqGroup extends AnnotatedSeqGroup {

  private final Das2VersionedSource original_version;

  public Das2SeqGroup(Das2VersionedSource version, String gid) {
    super(gid);
    original_version = version;
  }

  private void ensureSeqsLoaded()  {
    original_version.getSegments();
  }

  

  /**
   *  Returns a List of BioSeq objects.
   *  Will not return null.  The list is in the same order as in
   *  {@link #getSeq(int)}.
   */
    @Override
  public List<BioSeq> getSeqList() {
    ensureSeqsLoaded();
    return super.getSeqList();
  }

  /**
   *  Returns the sequence at the given position in the sequence list.
   */
  public BioSeq getSeq(int index) {
    ensureSeqsLoaded();
    return super.getSeq(index);
  }

  /** Returns the number of sequences in the group. */
  public int getSeqCount() {
    ensureSeqsLoaded();
    return super.getSeqCount();
  }


  /** Gets a sequence based on its name, possibly taking synonyms into account.
   *  See {@link #setUseSynonyms(boolean)}.
   */
  public BioSeq getSeq(String synonym) {
    ensureSeqsLoaded();
    return super.getSeq(synonym);

  }

  /**
   *  For the given symmetry, tries to find in the group a sequence
   *    that is pointed to by that symmetry.
   *  @return the first sequence it finds (by iterating through sym's spans),
   *    or null if none is found.
   */
  public BioSeq getSeq(SeqSymmetry sym) {
    ensureSeqsLoaded();
    return super.getSeq(sym);
  }

}
