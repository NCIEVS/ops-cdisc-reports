import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class OwlReader {

  private static final String OWL_FILE = "/Users/squareroot/wci/ncievs-CDISC/Thesaurus-220314-22.03b.owl";
  public Set<String> getConcepts() {
    Set<String> concepts = new HashSet<>();
    concepts.add("C165634");
    try (Scanner scanner =
        new Scanner(
            new File(
                "/Users/squareroot/wci/ncievs-CDISC/Current/Define-XML/Define-XML Terminology.txt")); ) {
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

  public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
    OwlReader reader = new OwlReader();
    Set<String> concepts = reader.getConcepts();
    Map<String, ConceptMarker> conceptMarkerMap = new HashMap<>();
    XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    XMLEventReader xmlReader =
        xmlInputFactory.createXMLEventReader(
            new FileInputStream(OWL_FILE));
    String currentConceptCode = null;

    while (xmlReader.hasNext()) {
      XMLEvent nextEvent = xmlReader.nextEvent();
      if (nextEvent.isStartElement()) {
        StartElement startElement = nextEvent.asStartElement();
        if (startElement.getName().getLocalPart().equals("Class")) {
          Attribute aboutAttribute =
              startElement.getAttributeByName(
                  new QName("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about", "rdf"));
          if (aboutAttribute == null) {
            continue;
          }
          currentConceptCode =
              aboutAttribute
                  .getValue()
                  .replace("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#", "");
          if (concepts.contains(currentConceptCode)) {
            int startLocation = startElement.getLocation().getLineNumber();
            conceptMarkerMap.put(currentConceptCode, new ConceptMarker(startLocation));
          }
        }
      }
      if (nextEvent.isEndElement()) {
        EndElement endElement = nextEvent.asEndElement();
        if (endElement.getName().getLocalPart().equals("Axiom")) {
          if (conceptMarkerMap.get(currentConceptCode) != null) {
            conceptMarkerMap.get(currentConceptCode).endLineNumber =
                endElement.getLocation().getLineNumber();
          }
        }
      }
    }
    int conceptNumber = 0;
    try (PrintWriter pw = new PrintWriter("/Users/squareroot/temp/Define.out")) {
      // <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#C132340 -->
      for (String conceptCode : conceptMarkerMap.keySet()) {
        // comment needed for the OWL parser
        System.out.println(String.format("Concept %d of %d",conceptNumber++, conceptMarkerMap.keySet().size()));
        ConceptMarker conceptMarker = conceptMarkerMap.get(conceptCode);
        pw.println(
            String.format(
                    "<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#%s -->", conceptCode));
        try (Stream<String> lines = Files.lines(Paths.get(OWL_FILE))) {
          //System.out.println(conceptCode);
          lines.skip(conceptMarker.startLineNumber-1).limit(conceptMarker.endLineNumber-conceptMarker.startLineNumber+1).forEach(pw::println);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
