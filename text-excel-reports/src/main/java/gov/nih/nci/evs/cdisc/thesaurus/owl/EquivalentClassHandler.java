package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class EquivalentClassHandler implements ClassElementHandler {
  private final static Logger log = LoggerFactory.getLogger(EquivalentClassHandler.class);

  @Override
  public boolean handle(XMLEvent event, Concept currentConcept) {
    log.trace(
        "Handle child elements of owl:equivalentClass for concept{}", currentConcept.getCode());
    if (event.isStartElement()) {
      StartElement startElement = event.asStartElement();
      QName qName = startElement.getName();
      if (ThesaurusOwlReader.RDF_NAMESPACE_PREFIX.equals(qName.getPrefix())
          && "Description".equals(qName.getLocalPart())) {
        currentConcept.getParents().add(ThesaurusOwlReader.getCodeFromAboutAttribute(startElement));
        return false;
      }
    }
    return true;
  }
}
