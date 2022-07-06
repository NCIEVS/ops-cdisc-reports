package gov.nih.nci.evs.cdisc.report;

public class HtmlReportGenerator {
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

    public static String generateHtmlReport(String odmXmlFile, ReportEnum reportType) {
        String htmlFileName = getHtmlFileName(odmXmlFile, reportType);
        String xsltFileName = getXsltFileName(reportType);
        XsltTransformer.convertXmlToHtml(odmXmlFile, htmlFileName, xsltFileName);
        return htmlFileName;
    }

  public static void main(String[] args) {
    if(args.length>=1){
        String owlFilePath = args[0];
        ReportEnum reportType = ReportEnum.MAIN_HTML;
        if (args.length == 2){
            reportType = ReportEnum.valueOf(args[1]);
        }
        generateHtmlReport(owlFilePath, reportType);
    } else {
      System.out.println("Expecting path to owl.xml file");
    }
  }
}
