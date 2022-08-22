package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class ConceptInSubsetHandler implements ClassElementHandler {
  @Override
  public boolean handle(XMLEvent event, Concept currentConcept) {
    if (event.isStartElement()) {
      StartElement startElement = event.asStartElement();
      String subsetCode = ThesaurusOwlReader.getCodeFromResource(startElement);
      if (subsetCode == null) {
        return false;
      }
      currentConcept.getSubsetCodes().add(subsetCode);
    }
    return false;
  }
}
