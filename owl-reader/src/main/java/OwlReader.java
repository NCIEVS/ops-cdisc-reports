import gov.nih.nci.evs.cdisc.report.model.Synonym;

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
import java.io.FileNotFoundException;
import java.util.*;

public class OwlReader {

  private static final String OWL_FILE =
      "/Users/squareroot/wci/ncievs-CDISC/Thesaurus-220314-22.03b.owl";
  private static final String THESAURUS_NAMESPACE =
      "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";

  private static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
  private static final String RETIRED_CONCEPT_STATUS = "Retired_Concept";
  private static final String PREFERRED_NAME_CODE = "P108";
  private static final String CONCEPT_STATUS_CODE = "P310";
  private static final String ALTERNATIVE_SOURCE_CODE = "P325";
  private static final String SYNONYM_CODE = "P90";
  private static final String TERM_GROUP_CODE = "P383";
  private static final String TERM_SOURCE_CODE = "P384";
  private static final String SOURCE_NAME_CODE = "P385";
  private static final String SUB_SOURCE_NAME_CODE = "P386";

  public Set<String> getConcepts() {
    Set<String> concepts = new HashSet<>();
    concepts.add("C165634");
    try (Scanner scanner =
        new Scanner(
            new File(
                "/Users/squareroot/wci/ncievs-CDISC/Current/Define-XML/Define-XML Terminology.txt"))) {
      // skip header
      scanner.nextLine();
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        try (Scanner rowScanner = new Scanner(line)) {
          rowScanner.useDelimiter("\t");
          concepts.add(rowScanner.next());
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return concepts;
  }

  //  public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
  //    OwlReader reader = new OwlReader();
  //    Set<String> concepts = reader.getConcepts();
  //    Map<String, ConceptMarker> conceptMarkerMap = new HashMap<>();
  //    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
  //    XMLEventReader xmlReader =
  //        xmlInputFactory.createXMLEventReader(
  //            new FileInputStream(OWL_FILE));
  //    String currentConceptCode = null;
  //
  //    while (xmlReader.hasNext()) {
  //      XMLEvent nextEvent = xmlReader.nextEvent();
  //      if (nextEvent.isStartElement()) {
  //        StartElement startElement = nextEvent.asStartElement();
  //        if (startElement.getName().getLocalPart().equals("Class")) {
  //          Attribute aboutAttribute =
  //              startElement.getAttributeByName(
  //                  new QName("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about", "rdf"));
  //          if (aboutAttribute == null) {
  //            continue;
  //          }
  //          currentConceptCode =
  //              aboutAttribute
  //                  .getValue()
  //                  .replace("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#", "");
  //          if (concepts.contains(currentConceptCode)) {
  //            int startLocation = startElement.getLocation().getLineNumber();
  //            conceptMarkerMap.put(currentConceptCode, new ConceptMarker(startLocation));
  //          }
  //        }
  //      }
  //      if (nextEvent.isEndElement()) {
  //        EndElement endElement = nextEvent.asEndElement();
  //        if (endElement.getName().getLocalPart().equals("Axiom")) {
  //          if (conceptMarkerMap.get(currentConceptCode) != null) {
  //            conceptMarkerMap.get(currentConceptCode).endLineNumber =
  //                endElement.getLocation().getLineNumber();
  //          }
  //        }
  //      }
  //    }
  //    int conceptNumber = 0;
  //    try (PrintWriter pw = new PrintWriter("/Users/squareroot/temp/Define.out")) {
  //      // <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C132340 -->
  //      for (String conceptCode : conceptMarkerMap.keySet()) {
  //        // comment needed for the OWL parser
  //        System.out.println(String.format("Concept %d of %d",conceptNumber++,
  // conceptMarkerMap.keySet().size()));
  //        ConceptMarker conceptMarker = conceptMarkerMap.get(conceptCode);
  //        pw.println(
  //            String.format(
  //                    "<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#%s -->",
  // conceptCode));
  //        try (Stream<String> lines = Files.lines(Paths.get(OWL_FILE))) {
  //          //System.out.println(conceptCode);
  //
  // lines.skip(conceptMarker.startLineNumber-1).limit(conceptMarker.endLineNumber-conceptMarker.startLineNumber+1).forEach(pw::println);
  //        }
  //      }
  //    } catch (IOException e) {
  //      e.printStackTrace();
  //    }
  //  }

  public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
    OwlReader reader = new OwlReader();
    Map<String, Concept> conceptMarkerMap = new HashMap<>();
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    XMLEventReader xmlReader = xmlInputFactory.createXMLEventReader(new FileInputStream(OWL_FILE));
    Concept currentConcept = null;
    Synonym currentSynonym = null;

    boolean capturePreferredNameText = false;
    boolean captureLabel = false;
    boolean captureConceptStatus = false;
    boolean captureSynonym = false;
    boolean captureAnnotatedTarget = false;
    boolean startAxiom = false;
    boolean startSynonym = false;
    boolean startAlternativeDefinition = false;

    boolean captureTermGroup = false;
    boolean captureTermSource = false;
    boolean captureSourceCode = false;
    boolean captureSubSourceName = false;

    while (xmlReader.hasNext()) {
      XMLEvent nextEvent = xmlReader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        if (startElement.getName().getLocalPart().equals("Class")) {
          currentConcept = new Concept();
          Attribute aboutAttribute =
              startElement.getAttributeByName(new QName(RDF_NAMESPACE, "about", "rdf"));
          if (aboutAttribute == null) {
            continue;
          }
          currentConcept.setCode(aboutAttribute.getValue().replace(THESAURUS_NAMESPACE, ""));
        }
        if (startElement.getName().getLocalPart().equals("subClassOf")) {
          String childCode = getCodeFromResource(startElement);
          if (childCode == null) {
            continue;
          }
          currentConcept.getChildCodes().add(childCode);
        }
        if (startElement.getName().getLocalPart().equals("A8")) {
          String subsetCode = getCodeFromResource(startElement);
          if (subsetCode == null) {
            continue;
          }
          currentConcept.getSubsetCodes().add(subsetCode);
        }
        if (startElement.getName().getLocalPart().equals(PREFERRED_NAME_CODE)
            && currentConcept != null) {
          capturePreferredNameText = true;
        }
        if (startElement.getName().getLocalPart().equals(CONCEPT_STATUS_CODE)
            && currentConcept != null) {
          captureConceptStatus = true;
        }
        if (startElement.getName().getLocalPart().equals(SYNONYM_CODE) && currentConcept != null) {
          currentSynonym = new Synonym();
          currentSynonym.setCode(currentConcept.getCode());
          captureSynonym = true;
        }
        if (startElement.getName().getLocalPart().equals("label")
            && startElement.getName().getNamespaceURI().equals(RDFS_NAMESPACE)
            && currentConcept != null) {
          captureLabel = true;
        }
        if (startElement.getName().getLocalPart().equals("Axiom") && currentConcept != null) {
          startAxiom = true;
        }
        if (startElement.getName().getLocalPart().equals("annotatedSource") && startAxiom) {
          String annotatedSource = getCodeFromResource(startElement);
          // The current concept's code does not match the Axiom's code.
          if (!currentConcept.getCode().equals(annotatedSource)) {
            throw new RuntimeException(
                String.format(
                    "Unexpected Axiom source. Expecting %s. Got %s",
                    currentConcept.getCode(), annotatedSource));
          }
        }
        if (startElement.getName().getLocalPart().equals("annotatedProperty") && startAxiom) {
          String annotatedProperty = getCodeFromResource(startElement);
          if (SYNONYM_CODE.equals(annotatedProperty)) {
            startSynonym = true;
          }
          if (ALTERNATIVE_SOURCE_CODE.equals(annotatedProperty)) {
            startAlternativeDefinition = true;
          }
        }
        if (startElement.getName().getLocalPart().equals("annotatedTarget") && startSynonym) {
          captureAnnotatedTarget = true;
        }
        if (startElement.getName().getLocalPart().equals(TERM_GROUP_CODE) && startSynonym) {
          captureTermGroup = true;
        }
        if (startElement.getName().getLocalPart().equals(TERM_SOURCE_CODE) && startSynonym) {
          captureTermSource = true;
        }
        if (startElement.getName().getLocalPart().equals(SOURCE_NAME_CODE) && startSynonym) {
          captureSourceCode = true;
        }
        if (startElement.getName().getLocalPart().equals(SUB_SOURCE_NAME_CODE) && startSynonym) {
          captureSubSourceName = true;
        }
      }

      if (nextEvent.isCharacters()) {
        String data = nextEvent.asCharacters().getData();
        if (capturePreferredNameText) {
          currentConcept.setPreferredName(data);
          capturePreferredNameText = false;
        }
        if (captureLabel) {
          currentConcept.setLabel(data);
          captureLabel = false;
        }
        if (captureConceptStatus) {
          currentConcept.setRetired(data.equals(RETIRED_CONCEPT_STATUS));
          captureConceptStatus = false;
        }
        if (captureSynonym) {
          currentSynonym.setLabel(data);
          captureSynonym = false;
        }
        if (captureAnnotatedTarget) {
          currentSynonym.setTermName(data);
          captureAnnotatedTarget = false;
        }
        if (captureTermGroup) {
          currentSynonym.setTermGroup(data);
          captureTermGroup = false;
        }
        if (captureTermSource) {
          currentSynonym.setTermSource(data);
          captureTermSource = false;
        }
        if (captureSourceCode) {
          currentSynonym.setSourceCode(data);
          captureSourceCode = false;
        }
        if (captureSubSourceName) {
          currentSynonym.setSubSourceName(data);
          captureSubSourceName = false;
        }
      }
      if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals("Axiom")) {
          startAxiom = false;
          currentSynonym = null;
        }
      }
    }
  }

  private static String getCodeFromResource(StartElement element) {
    Attribute subClassResource =
        element.getAttributeByName(new QName(RDF_NAMESPACE, "resource", "rdf"));
    if (subClassResource == null) {
      return null;
    }
    return subClassResource.getValue().replace(THESAURUS_NAMESPACE, "");
  }
}
