package com.affymetrix.genometryImpl.parsers.das;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author sgblanch
 * @version $Id: DASFeatureParser.java 5979 2010-05-21 16:42:02Z jnicol $
 */
public final class DASFeatureParser {

	static enum Orientation { UNKNOWN, FORWARD, REVERSE };
	private static enum Elements {
		DASGFF, GFF, SEGMENT, FEATURE, TYPE, METHOD, START, END, SCORE, ORIENTATION, PHASE, NOTE, LINK, TARGET, GROUP
	};
	private static enum Attr {
		version, href, id, start, stop, type, label, category, reference
	};

	private BioSeq sequence;
	private String note;
	private boolean annotateSeq = true;

	public void setAnnotateSeq(boolean annotateSeq) {
		this.annotateSeq = annotateSeq;
	}
	
	public Collection<DASSymmetry> parse(InputStream s, AnnotatedSeqGroup seqGroup) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLEventReader reader = factory.createXMLEventReader(s);
		XMLEvent current;
		Deque<StartElement> stack = new ArrayDeque<StartElement>();
		FeatureBean feature = new FeatureBean();
		LinkBean link = new LinkBean();
		GroupBean group = new GroupBean();
		TargetBean target = new TargetBean();
		Map<String, DASSymmetry> groupMap = new HashMap<String, DASSymmetry>();

		while(reader.hasNext()) {
			current = reader.nextEvent();
			switch(current.getEventType()) {
				case XMLEvent.START_ELEMENT:
					startElement(current.asStartElement(), feature, link, group, target, seqGroup);
					stack.push(current.asStartElement());
					break;
				case XMLEvent.CHARACTERS:
					characters(current.asCharacters(), stack.peek(), feature, link, target);
					break;
				case XMLEvent.END_ELEMENT:
					stack.pop();
					endElement(current.asEndElement(), stack.peek(), groupMap, feature, link, group, target, seqGroup);
					break;
			}
		}

		return groupMap.values();
	}

	/**
	 * Handle an XML start tag.  This creates various storage beans when
	 * necessary and stores XML attributes in the appropriate bean.
	 *
	 * @param current
	 * @param seqGroup
	 */
	private void startElement(StartElement current, FeatureBean feature, LinkBean link, GroupBean group, final TargetBean target, AnnotatedSeqGroup seqGroup) {
		switch (Elements.valueOf(current.getName().getLocalPart())) {
			case SEGMENT:
				sequence = seqGroup.addSeq(getAttribute(current, Attr.id),
						Integer.valueOf(getAttribute(current, Attr.stop)));
				break;
			case FEATURE:
				feature.clear();
				feature.setID(getAttribute(current, Attr.id));
				feature.setLabel(getAttribute(current, Attr.label));
				break;
			case TYPE:
				feature.setTypeID(getAttribute(current, Attr.id));
				feature.setTypeCategory(getAttribute(current, Attr.category));
				feature.setTypeReference(getAttribute(current, Attr.reference));
				break;
			case METHOD:
				feature.setMethodID(getAttribute(current, Attr.id));
				break;
			case LINK:
				link.clear();
				link.setURL(getAttribute(current, Attr.href));
				break;
			case TARGET:
				target.clear();
				target.setID(getAttribute(current, Attr.id));
				target.setStart(getAttribute(current, Attr.start));
				target.setStop(getAttribute(current, Attr.stop));
				break;
			case GROUP:
				group.clear();
				group.setID(getAttribute(current, Attr.id));
				group.setLabel(getAttribute(current, Attr.label));
				group.setType(getAttribute(current, Attr.type));
		}
	}

	/**
	 * Handle XML character data.  This stores the data in the correct bean,
	 * depending on what tag we are processing.
	 *
	 * @param current
	 * @param parent
	 */
	private void characters(Characters current, StartElement parent, FeatureBean feature, LinkBean link, TargetBean target) {
		switch (Elements.valueOf(parent.getName().getLocalPart())) {
			case TYPE:
				feature.setTypeLabel(current.getData());
				break;
			case METHOD:
				feature.setMethodLabel(current.getData());
				break;
			case START:
				feature.setStart(current.getData());
				break;
			case END:
				feature.setEnd(current.getData());
				break;
			case SCORE:
				feature.setScore(current.getData());
				break;
			case ORIENTATION:
				feature.setOrientation(current.getData());
				break;
			case PHASE:
				feature.setPhase(current.getData());
				break;
			case NOTE:
				note = current.getData();
				break;
			case LINK:
				link.setTitle(current.getData());
				break;
			case TARGET:
				target.setName(current.getData());
				break;
		}
	}

	/**
	 * Handle an XML end tag.  This stores certain child beans in their parent
	 * beans.  It will also create a SeqSymmetry when finished with a feature
	 * tag.
	 *
	 * @param current
	 * @param parent
	 */
	private void endElement(EndElement current, StartElement parent, Map<String, DASSymmetry> groupMap, FeatureBean feature, LinkBean link, GroupBean group, TargetBean target, AnnotatedSeqGroup seqGroup) {
		Elements p = null;
		if (parent != null) {
			p = Elements.valueOf(parent.getName().getLocalPart());
		}
		switch (Elements.valueOf(current.getName().getLocalPart())) {
			case FEATURE:
				DASSymmetry groupSymmetry;
				DASSymmetry featureSymmetry = new DASSymmetry(feature, sequence);

				if (feature.getGroups().isEmpty()) {
					if (annotateSeq) {
						sequence.addAnnotation(featureSymmetry);
						groupMap.put(featureSymmetry.getID(), featureSymmetry);
					}
				} else {
					for (GroupBean groupBean : feature.getGroups()) {
						groupSymmetry = getGroupSymmetry(groupMap, feature, groupBean, seqGroup);
						groupSymmetry.addChild(featureSymmetry);
					}
				}
				break;
			case NOTE:
				if (p == Elements.FEATURE) {
					feature.addNote(note);
				} else if (p == Elements.GROUP) {
					group.addNote(note);
				}
				break;
			case LINK:
				if (p == Elements.FEATURE) {
					feature.addLink(link);
				} else if (p == Elements.GROUP) {
					group.addLink(link);
				}
				break;
			case TARGET:
				if (p == Elements.FEATURE) {
					feature.addTarget(target);
				} else if (p == Elements.GROUP) {
					group.addTarget(target);
				}
				break;
			case GROUP:
				feature.addGroup(group);
				break;
		}
	}

	private static String getAttribute(StartElement current, Attr attr) {
		QName qName = new QName(current.getName().getNamespaceURI(), attr.toString());
		Attribute attribute = current.getAttributeByName(qName);
		return attribute == null ? "" : attribute.getValue();
	}

	private DASSymmetry getGroupSymmetry(Map<String, DASSymmetry> groupMap, FeatureBean feature, GroupBean group, AnnotatedSeqGroup seqGroup) {
		/* Do we have a groupSymmetry for ID stored in parser */
		if (groupMap.containsKey(group.getID())) {
			return groupMap.get(group.getID());
		}

		/* Is there a groupSymmetry for ID on this sequence */
		for (SeqSymmetry sym : seqGroup.findSyms(group.getID())) {
			if (sym instanceof DASSymmetry && sym.getSpan(sequence) != null) {
				groupMap.put(sym.getID(), (DASSymmetry) sym);
				return (DASSymmetry) sym;
			}
		}

		/* Create a new groupSymmetry for ID */
		DASSymmetry groupSymmetry = new DASSymmetry(group, feature, sequence);
		if (annotateSeq) {
			sequence.addAnnotation(groupSymmetry);
			seqGroup.addToIndex(groupSymmetry.getID(), groupSymmetry);
		}
		groupMap.put(groupSymmetry.getID(), groupSymmetry);

		return groupSymmetry;
	}

}
