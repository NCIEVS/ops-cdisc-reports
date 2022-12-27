package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class SubClassOfHandler implements ClassElementHandler {

    @Override
    public boolean handle(XMLEvent event, Concept currentConcept) {
        if(event.isStartElement()){
            StartElement startElement = event.asStartElement();
            String childCode = ThesaurusOwlReader.getCodeFromResource(startElement);
            if (childCode == null) {
                return false;
            }
            currentConcept.getParents().add(childCode);
        }
        return false;
    }
}
