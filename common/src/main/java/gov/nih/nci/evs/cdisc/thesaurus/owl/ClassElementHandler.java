package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;

import javax.xml.stream.events.XMLEvent;

@FunctionalInterface
public interface ClassElementHandler {
    boolean handle(XMLEvent event, Concept currentConcept);
}
