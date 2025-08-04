package gov.nih.nci.evs.cdisc.report;

public class IchHtmlReportGenerator extends HtmlReportGenerator {

    @Override
    protected String getXsltFileName(ReportEnum reportType) {
        return String.format(
                "/xslt/%s",
                ReportEnum.MAIN_HTML.equals(reportType)
                        ? "ich.xsl"
                        : "ich-pdf.xsl");

    }
}
