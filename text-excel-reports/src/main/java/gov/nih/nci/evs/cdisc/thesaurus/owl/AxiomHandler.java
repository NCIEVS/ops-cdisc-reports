package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.thesaurus.model.AlternativeDefinition;
import gov.nih.nci.evs.cdisc.thesaurus.model.Axiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class AxiomHandler {
  private static final Logger log = LoggerFactory.getLogger(AxiomHandler.class);

  protected static final String ALTERNATIVE_SOURCE_CODE = "P325";
  protected static final String SYNONYM_CODE = "P90";
  protected static final String TERM_GROUP_CODE = "P383";
  protected static final String TERM_SOURCE_CODE = "P384";
  protected static final String SOURCE_NAME_CODE = "P385";
  protected static final String SUB_SOURCE_NAME_CODE = "P386";
  protected static final String DEFINITION_SOURCE = "P378";

  private boolean captureTarget;
  private final Axiom axiom = new Axiom();
  private String currentElementName;

  /**
   * Handles an axiom element start event. All child elements in axiom element will be handled here
   *
   * @param event required, current element within axiom
   * @return only certain properties are captured. If the property is not captured false will be
   *     returned. The calling method can stop calling this handler for subsequent events
   */
  public boolean handleAxiom(XMLEvent event) {
    log.trace(
        "Handling axiom tag for {}",
        axiom.getAnnotatedSource() != null ? axiom.getAnnotatedSource() : "UNKNOWN");
    if (event.isStartElement()) {
      StartElement startElement = event.asStartElement();
      if (startElement.getName().getLocalPart().equals("annotatedSource")) {
        String source = ThesaurusOwlReader.getCodeFromResource(startElement);
        axiom.setAnnotatedSource(source);
      } else if (startElement.getName().getLocalPart().equals("annotatedProperty")) {
        String property = ThesaurusOwlReader.getCodeFromResource(startElement);
        if (!SYNONYM_CODE.equals(property) && !ALTERNATIVE_SOURCE_CODE.equals(property)) {
          // Short Circuit processing of Axiom if it is not a source we care about
          log.trace(
              "property {} is not being captured. concept code:{}",
              property,
              axiom.getAnnotatedSource());
          return false;
        }
        axiom.setAnnotatedProperty(property);
      } else if (startElement.getName().getLocalPart().equals("annotatedTarget")) {
        captureTarget = true;
      } else {
        currentElementName = startElement.getName().getLocalPart();
      }
    } else if (event.isCharacters()) {
      String data = event.asCharacters().getData();
      if (captureTarget) {
        StringBuilder existingData = getExistingData(axiom.getAnnotatedTarget());
        axiom.setAnnotatedTarget(existingData.append(data).toString());
      } else if (currentElementName != null) {
        StringBuilder existingData = getExistingData(axiom.getElements().get(currentElementName));
        axiom.getElements().put(currentElementName, existingData.append(data).toString());
      }
    } else if (event.isEndElement()) {
      // To handle character events for end of line characters after end of element
      currentElementName = null;
      captureTarget = false;
    }

    return true;
  }

  private StringBuilder getExistingData(String existingData) {
    return existingData != null ? new StringBuilder(existingData) : new StringBuilder();
  }

  public Object convert(String label) {
    if (SYNONYM_CODE.equals(axiom.getAnnotatedProperty())) {
      Synonym synonym = new Synonym();
      synonym.setLabel(label);
      synonym.setCode(axiom.getAnnotatedSource());
      synonym.setTermName(axiom.getAnnotatedTarget());
      synonym.setTermGroup(axiom.getElements().get(TERM_GROUP_CODE));
      synonym.setTermSource(axiom.getElements().get(TERM_SOURCE_CODE));
      synonym.setSourceCode(axiom.getElements().get(SOURCE_NAME_CODE));
      synonym.setSubSourceName(axiom.getElements().get(SUB_SOURCE_NAME_CODE));
      return synonym;
    } else if (ALTERNATIVE_SOURCE_CODE.equals(axiom.getAnnotatedProperty())) {
      AlternativeDefinition alternativeDefinition = new AlternativeDefinition();
      alternativeDefinition.setConceptCode(axiom.getAnnotatedSource());
      alternativeDefinition.setDefinition(axiom.getAnnotatedTarget());
      String definitionSource = axiom.getElements().get(DEFINITION_SOURCE);
      if (definitionSource != null) {
        if ("CDISC".equals(definitionSource)) {
          alternativeDefinition.setCdisc(true);
        } else if ("CDISC-GLOSS".equals(definitionSource)) {
          alternativeDefinition.setCdisc(false);
        } else {
          return null;
        }
      }
      return alternativeDefinition;
    }
    return null;
  }
}
