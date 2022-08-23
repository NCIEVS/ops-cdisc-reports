package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;

public class CharacterHandlerFactory {

  private CharacterHandlerFactory() {}

  public static CharacterHandlerFactory getInstance() {
    return new CharacterHandlerFactory();
  }

  public CharacterHandler getCharacterHandler(String code) {
    if (ClassElementEnum.PREFERRED_NAME.getLocalPart().equals(code)) {
      return new PreferredTermHandler();
    }
    return null;
  }

  private class PreferredTermHandler extends CharacterHandler {
    @Override
    public void setValue(Concept concept, String data) {
      concept.setPreferredName(appendData(concept.getPreferredName(), data));
    }
  }

  private String appendData(String existingData, String data) {
    StringBuilder sbData = existingData != null ? new StringBuilder(existingData) : new StringBuilder();
    return sbData.append(data).toString();
  }
}
