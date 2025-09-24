package gov.nih.nci.evs.cdisc.report;

public class IchHtmlReportGenerator extends HtmlReportGenerator {

    @Override
    protected String getXsltFileName(ReportEnum reportType) {
        return String.format(
                "/xslt/%s",
                ReportEnum.MAIN_HTML.equals(reportType)
                        ? "controlledterminology1-0-0-m11.xsl"
                        : "controlledterminology1-0-0-m11-pdf.xsl");

    }
}
