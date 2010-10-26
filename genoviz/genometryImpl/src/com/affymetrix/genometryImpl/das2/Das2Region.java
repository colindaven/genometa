package com.affymetrix.genometryImpl.das2;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import java.net.URI;

public final class Das2Region {

    private final URI region_uri;
    private  BioSeq aseq;
    private final Das2VersionedSource versioned_source;

    public Das2Region(Das2VersionedSource source, URI reg_uri, String name, String info, int len) {
        region_uri = reg_uri;

        versioned_source = source;
        AnnotatedSeqGroup genome = versioned_source.getGenome();
        // a)  see if id of Das2Region maps directly to an already seen annotated seq in genome
        //   check for prior existence of BioSeq for Das2Region only if genome group is _not_ a Das2SeqGroup
        //      if group is a Das2SeqGroup, then calling getSeq() will trigger infinite loop as group attempts
        //      to initialize sequences via Das2VersionedSources.getSegments()
        //   But if genome is a Das2SeqGroup, then can assume that no seqs are in group that aren't
        //      being put there in this constructor, and these will be unique, so can skip check for prior existence
        if (!(genome instanceof Das2SeqGroup)) {
            aseq = genome.getSeq(name);
            if (aseq == null) {
                aseq = genome.getSeq(this.getID());
            }
        }
        // b) if can't find a previously seen genome for this DasSource, then
        //     create a new genome entry
        if (aseq == null) {
            // using name instead of id for now
            aseq = genome.addSeq(name, len);
        }
    }

    public String getID() {
        return region_uri.toString();
    }

    public Das2VersionedSource getVersionedSource() {
        return versioned_source;
    }

    public BioSeq getAnnotatedSeq() {
        return aseq;
    }

}
