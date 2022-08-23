package gov.nih.nci.evs.cdisc.thesaurus.owl;

import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.thesaurus.model.AlternativeDefinition;
import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the entire NCI Thesaurus file. Uses a SAX parser to read through the file and holds all the
 * concepts found the in the owl file. No self defining metadata is stored
 */
public class ThesaurusOwlReader {
  private static final Logger log = LoggerFactory.getLogger(ThesaurusOwlReader.class);
  private static final String THESAURUS_NAMESPACE =
      "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";

  private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  protected static final String RETIRED_CONCEPT_STATUS = "Retired_Concept";
  protected static final String RDF_NAMESPACE_PREFIX = "rdf";
  protected static final String RDF_SCHEMA_NAMESPACE_PREFIX = "rdfs";
  protected static final String OWL_NAMESPACE_PREFIX = "owl";

  private final File owlFile;

  public ThesaurusOwlReader(File owlFile) {
    this.owlFile = owlFile;
  }

  public Map<String, Concept> createConceptMap() {
    log.info("Creating concept map for {}", owlFile);
    Map<String, Concept> conceptMap = new LinkedHashMap<>();
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    try (InputStream owlStream = new FileInputStream(owlFile)) {
      XMLEventReader xmlReader = xmlInputFactory.createXMLEventReader(owlStream);
      populateConceptMap(conceptMap, xmlReader);
    } catch (Exception e) {
      throw new RuntimeException("Exception occurred while creating concept map", e);
    }
    return conceptMap;
  }

  private void populateConceptMap(Map<String, Concept> conceptMap, XMLEventReader xmlReader)
      throws XMLStreamException {
    Concept currentConcept = null;
    AxiomHandler axiomHandler = new AxiomHandler();
    ConceptHandler conceptHandler = new ConceptHandler();

    boolean handleClass = false;
    boolean handleAxiom = false;
    /**
     * There is an owl:Class child element in the owl:equivalentClass or owl:subClass elements. In
     * order to not stop reading the top level owl:Class when we hit the endElement for these child
     * elements, we maintain the depth level of where the owl:Class element was found and end
     * processing the top-level class only when a depth of zero is reached
     */
    int classDepth = 0;

    while (xmlReader.hasNext()) {
      XMLEvent nextEvent = xmlReader.nextEvent();
      if (handleClass) {
        log.trace("Processing elements for code:{}", currentConcept.getCode());
        conceptHandler.handle(nextEvent, currentConcept);
      }
      if (handleAxiom) {
        boolean continueAxiom = axiomHandler.handleAxiom(nextEvent);
        if (!continueAxiom) {
          handleAxiom = false;
          continue;
        }
      }
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        if (isClass(startElement.getName())) {
          String code = getCodeFromAboutAttribute(startElement);
          if (code != null) {
            log.trace("Processing concept code:{}", code);
            currentConcept = new Concept();
            currentConcept.setCode(code);
            handleClass = true;
            conceptHandler = new ConceptHandler();
          }
          classDepth++;
        }
        if (isAxiom(startElement.getName(), currentConcept)) {
          log.trace(
              "Processing axiom for code:{}",
              currentConcept != null ? currentConcept.getCode() : "UNKNOWN");
          handleAxiom = true;
          axiomHandler = new AxiomHandler();
        }
      }
      if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (isClass(endElement.getName())) {
          classDepth--;
          // Reached top level owl:Class element's end tag. So store concept and reset
          if (classDepth == 0) {
            log.trace(
                "Completed processing {}",
                currentConcept != null ? currentConcept.getCode() : "UNKNOWN");
            handleClass = false;
            conceptMap.put(currentConcept.getCode(), currentConcept);
            if (conceptMap.size() % 1000 == 0) {
              log.info("Completed processing of {}", conceptMap.size());
            }
          }
        }
        if (isAxiom(endElement.getName(), currentConcept)) {
          handleAxiomEndTag(axiomHandler, currentConcept);
          handleAxiom = false;
          axiomHandler = null;
        }
      }
    }
  }

  protected static String getCodeFromResource(StartElement element) {
    Attribute subClassResource =
        element.getAttributeByName(new QName(RDF_NAMESPACE, "resource", RDF_NAMESPACE_PREFIX));
    if (subClassResource == null) {
      return null;
    }
    return subClassResource.getValue().replace(THESAURUS_NAMESPACE, "");
  }

  protected static String getCodeFromAboutAttribute(StartElement startElement) {
    Attribute aboutAttribute =
        startElement.getAttributeByName(new QName(RDF_NAMESPACE, "about", RDF_NAMESPACE_PREFIX));
    if (aboutAttribute == null) {
      return null;
    }
    return aboutAttribute.getValue().replace(THESAURUS_NAMESPACE, "");
  }

  private static boolean isAxiom(QName qName, Concept currentConcept) {
    return qName.getLocalPart().equals("Axiom")
        && qName.getPrefix().equals("owl")
        && currentConcept != null;
  }

  private static boolean isClass(QName qName) {
    return qName.getLocalPart().equals("Class") && qName.getPrefix().equals("owl");
  }

  private static void handleAxiomEndTag(AxiomHandler axiomHandler, Concept concept) {
    Object axiom = axiomHandler.convert(concept.getLabel());
    if (axiom != null) {
      if (axiom instanceof Synonym) {
        log.trace("Completed handling Axiom for {}. Axiom is a Synonym", concept.getCode());
        concept.getSynonyms().add((Synonym) axiom);
      } else {
        log.trace(
            "Completed handling Axiom for {}. Axiom is an Alternative Definition",
            concept.getCode());
        concept.getAlternativeDefinitions().add((AlternativeDefinition) axiom);
      }
    }
  }
}
