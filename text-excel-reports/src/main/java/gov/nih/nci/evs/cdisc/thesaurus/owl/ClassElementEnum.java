package gov.nih.nci.evs.cdisc.thesaurus.owl;

import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.function.Supplier;

public enum ClassElementEnum {
  SUBCLASS(
      ThesaurusOwlReader.RDF_SCHEMA_NAMESPACE_PREFIX, "subClassOf", SubClassOfHandler::new),
  CONCEPT_IN_SUBSET("A8", ConceptInSubsetHandler::new),
  EQUIVALENT_CLASS(
      ThesaurusOwlReader.OWL_NAMESPACE_PREFIX,
      "equivalentClass",
          EquivalentClassHandler::new),
  PREFERRED_NAME("P108", () -> CharacterHandlerFactory.getInstance().getCharacterHandler("P108")),
  CONCEPT_STATUS_CODE(
      "P310",
      () ->
          ((event, currentConcept) -> {
            if (event.isCharacters()) {
              String data = event.asCharacters().getData();
              currentConcept.setRetired(data.equals(ThesaurusOwlReader.RETIRED_CONCEPT_STATUS));
            }
            return false;
          })),
  EXTENSIBLE(
      "P361",
      () ->
          ((event, currentConcept) -> {
            if (event.isCharacters()) {
              String data = event.asCharacters().getData();
              if ("Yes".equals(data)) {
                currentConcept.setExtensible(true);
              } else if ("No".equals(data)) {
                currentConcept.setExtensible(false);
              }
            }
            return false;
          })),

  LABEL(
      ThesaurusOwlReader.RDF_SCHEMA_NAMESPACE_PREFIX,
      "label",
      () ->
          ((event, currentConcept) -> {
            if (event.isCharacters()) {
              String data = event.asCharacters().getData();
              currentConcept.setLabel(data);
            }
            return false;
          }));

  private String prefix;
  private final String localPart;
  private final Supplier<ClassElementHandler> handler;

  ClassElementEnum(String localPart, Supplier<ClassElementHandler> handler) {
    this.localPart = localPart;
    this.handler = handler;
  }

  ClassElementEnum(String prefix, String localPart, Supplier<ClassElementHandler> handler) {
    this.prefix = prefix;
    this.localPart = localPart;
    this.handler = handler;
  }

  public String getLocalPart() {
    return localPart;
  }

  public Supplier<ClassElementHandler> getHandler() {
    return handler;
  }

  public static ClassElementEnum fromEvent(QName qName) {
    return Arrays.stream(ClassElementEnum.values())
        .filter(
            propertyEnum ->
                qName.getLocalPart().equals(propertyEnum.localPart)
                    && ((StringUtils.isBlank(qName.getPrefix()) && propertyEnum.prefix == null)
                        || qName.getPrefix().equals(propertyEnum.prefix)))
        .findFirst()
        .orElse(null);
  }
}
