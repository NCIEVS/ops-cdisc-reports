package gov.nih.nci.evs.cdisc.report.utils;

import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/** A utility class to validate an XML file against an XSD schema. */
public class XmlValidator {

  /**
   * Validates an XML file against an XSD file.
   *
   * @param xmlFile The File object representing the XML to validate.
   * @param xsdFile The File object representing the XSD schema.
   * @return true if the XML is valid, false otherwise.
   */
  public static boolean validate(File xmlFile, File xsdFile) {
    try {
      // 1. Create a SchemaFactory capable of understanding W3C XML Schemas (XSD).
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

      // 2. Load the XSD schema.
      Schema schema = factory.newSchema(xsdFile);

      // 3. Create a Validator from the schema.
      Validator validator = schema.newValidator();

      // 4. Validate the XML file.
      validator.validate(new StreamSource(xmlFile));

      // If no exception is thrown, the XML is valid.
      return true;
    } catch (SAXException e) {
      // SAXException is thrown if validation fails.
      System.err.println("XML is NOT valid. Reason: " + e.getMessage());
      return false;
    } catch (IOException e) {
      // IOException is thrown if the files cannot be read.
      System.err.println("Error reading files: " + e.getMessage());
      return false;
    }
  }

  /** A simple main method to demonstrate the validation functionality. */
  public static void main(String[] args) {
    XmlValidator validator = new XmlValidator();
    File xmlFile = new File("/Users/squareroot/temp/ICH M11 5/ICH M11 Terminology.odm.xml");
    File xsdFile =
        new File(
            "/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/odm-report/src/main/resources/schema/controlled-terminology-m11/controlledterminology1-2-0-m11.xsd");
    boolean isValid = validator.validate(xmlFile, xsdFile);
    System.out.println("Is the XML valid? " + isValid);
  }
}
