package gov.nih.nci.evs.cdisc.report;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

public class HtmlReportGenerator {
  /**
   * Generates a HTML file for a given ODM XML file using XSLT
   *
   * @param odmXmlFile required, path to the ODM XML file
   * @param reportType required, this determines the contents of the HTML file
   * @return path to HTML report
   */
  public static String generateHtmlReport(String odmXmlFile, ReportEnum reportType) {
    assertRequired(odmXmlFile, "odmXmlFile");
    assertRequired(reportType, "reportType");
    String htmlFileName = getHtmlFileName(odmXmlFile, reportType);
    String xsltFileName = getXsltFileName(reportType);
    XsltTransformer.convertXmlToHtml(odmXmlFile, htmlFileName, xsltFileName);
    return htmlFileName;
  }

  private static String getHtmlFileName(String owlFileName, ReportEnum reportType) {
    return ReportEnum.MAIN_HTML.equals(reportType)
        ? owlFileName.replace(".odm.xml", ".html")
        : owlFileName.replace(".odm.xml", "-pdf.html");
  }

  private static String getXsltFileName(ReportEnum reportType) {
    return String.format(
        "/xslt/%s",
        ReportEnum.MAIN_HTML.equals(reportType)
            ? "controlledterminology1-0-0.xsl"
            : "controlledterminology1-0-0-pdf.xsl");
  }

  public static void main(String[] args) {
    if (args.length >= 1) {
      String odmXmlPath = args[0];
      ReportEnum reportType = ReportEnum.MAIN_HTML;
      if (args.length == 2) {
        reportType = ReportEnum.valueOf(args[1]);
      }
      generateHtmlReport(odmXmlPath, reportType);
    } else {
      System.out.println("Expecting path to owl.xml file");
    }
  }
}
