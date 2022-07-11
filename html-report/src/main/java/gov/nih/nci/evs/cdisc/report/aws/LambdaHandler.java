package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.ReportEnum;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static gov.nih.nci.evs.cdisc.report.HtmlReportGenerator.generateHtmlReport;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  /**
   * Creates HTML reports from the ODM XML file created by odm-report. Uses XSLT to transform the XML
   * to HTML. This lambda creates a standalone HTML report and an HTML report that will be converted
   * in PDF in another process
   *
   * @param input contains path to ODM XML report file
   * @param context lambda context
   * @return augmented report list with the standalone HTML and HTML for PDF reports
   */
  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    validate(input);
    for (ReportDetail reportDetail : input.getReportDetails()) {
      String odmXmlFile = reportDetail.getReports().get(ReportEnum.ODM_XML);
      if (odmXmlFile != null && new File(odmXmlFile).exists()) {
        reportDetail
            .getReports()
            .put(ReportEnum.MAIN_HTML, generateHtmlReport(odmXmlFile, ReportEnum.MAIN_HTML));
        reportDetail
            .getReports()
            .put(ReportEnum.PDF_HTML, generateHtmlReport(odmXmlFile, ReportEnum.PDF_HTML));
      } else {
        throw new RuntimeException(String.format("File %s does not exist.", odmXmlFile));
      }
    }
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(request.getPublicationDate(), "publicationDate");
  }
}
