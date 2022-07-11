package gov.nih.nci.evs.cdisc.report;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.FileWriter;
import java.io.StringWriter;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

public class XsltTransformer {
  /**
   * Converts ODM XML to HTML using XSLT
   *
   * @param xmlFilePath required, ODM XML path
   * @param htmlFilePath required, HTML report output path
   * @param xsltFilePath required, XSLT transformation file path
   */
  public static void convertXmlToHtml(
      String xmlFilePath, String htmlFilePath, String xsltFilePath) {
    assertRequired(xmlFilePath, "xmlFilePath");
    assertRequired(htmlFilePath, "htmlFilePath");
    assertRequired(xsltFilePath, "xsltFilePath");
    StringWriter sw = new StringWriter();
    Source xslt = new StreamSource(XsltTransformer.class.getResource(xsltFilePath).getPath());
    Source xml = new StreamSource(xmlFilePath);
    try (FileWriter fw = new FileWriter(htmlFilePath)) {
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transform = tFactory.newTransformer(xslt);
      transform.transform(xml, new StreamResult(sw));
      fw.write(sw.toString());
    } catch (Exception e) {
      throw new RuntimeException(
          String.format(
              "Error occurred when converting XML to HTML. xml:%s xslt:%s",
              xmlFilePath, xsltFilePath),
          e);
    }
  }
}
