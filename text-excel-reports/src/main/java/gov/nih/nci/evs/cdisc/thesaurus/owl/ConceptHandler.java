package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class ConceptHandler {
  private static final Logger log = LoggerFactory.getLogger(ConceptHandler.class);
  private ClassElementEnum classElementEnum;
  private boolean continueHandler;

  public void handle(XMLEvent event, Concept currentConcept) {
    if (event.isStartElement()) {
      StartElement startElement = event.asStartElement();
      if (!continueHandler) {
        classElementEnum = ClassElementEnum.fromEvent(startElement.getName());
      } else {
        log.trace(
            "Current handler is skipping processing of this event. Element: {}",
            startElement.getName());
      }
      if (classElementEnum == null) {
        return;
      } else {
        continueHandler = classElementEnum.getHandler().get().handle(event, currentConcept);
      }
    } else if (event.isEndElement()) {
      // To handle character events for end of line characters after end of element
      if (!continueHandler) {
        if (classElementEnum != null) {
          classElementEnum.getHandler().get().handle(event, currentConcept);
          classElementEnum = null;
        }
      }
    } else {
      if (classElementEnum != null) {
        classElementEnum.getHandler().get().handle(event, currentConcept);
      }
    }
  }
}
