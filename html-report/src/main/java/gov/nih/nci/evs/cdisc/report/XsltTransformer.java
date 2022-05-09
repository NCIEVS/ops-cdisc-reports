package gov.nih.nci.evs.cdisc.report;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.FileWriter;
import java.io.StringWriter;

public class XsltTransformer {
  public static void convertXmlToHtml(
      String xmlFilePath, String htmlFilePath, String xsltFilePath) {
    StringWriter sw = new StringWriter();
    Source xslt = new StreamSource(XsltTransformer.class.getResource(xsltFilePath).getPath());
    Source xml = new StreamSource(xmlFilePath);
    try (FileWriter fw = new FileWriter(htmlFilePath)){
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transform = tFactory.newTransformer(xslt);
      transform.transform(xml, new StreamResult(sw));
      fw.write(sw.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    //XsltTransformer.convertXmlToHtml("/Users/squareroot/wci/ncievs-CDISC/Current/ADaM/ADaM Terminology.odm.xml", "/Users/squareroot/temp/ADaM Terminology.html", "/xslt/controlledterminology1-0-0.xsl");
    XsltTransformer.convertXmlToHtml("/Users/squareroot/wci/ncievs-CDISC/Current/ADaM/ADaM Terminology.odm.xml", "/Users/squareroot/temp/ADaM Terminology-pdf.html", "/xslt/controlledterminology1-0-0-pdf.xsl");
  }
}
