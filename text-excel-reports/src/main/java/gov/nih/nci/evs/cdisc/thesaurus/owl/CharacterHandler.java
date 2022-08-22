package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;

import javax.xml.stream.events.XMLEvent;

public abstract class CharacterHandler implements ClassElementHandler {
  private final StringBuilder existingData = new StringBuilder();

  public String getExistingData() {
    return existingData.toString();
  }

  @Override
  public boolean handle(XMLEvent event, Concept currentConcept) {
    if (event.isCharacters()) {
      String data = event.asCharacters().getData();
      setValue(currentConcept, data);
    }
    return false;
  }

  public abstract void setValue(Concept concept, String data);

}
