package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.PdfHtmlFooter;
import gov.nih.nci.evs.cdisc.report.ReportEnum;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    log.info("Starting PDF report generation");
    validate(input);
    for (ReportDetail reportDetail : input.getReportDetails()) {
      log.info("PDF report generation for {}", reportDetail.getLabel());
      String pdfHtmlFile = reportDetail.getReports().get(ReportEnum.PDF_HTML);
      if (pdfHtmlFile != null && new File(pdfHtmlFile).exists()) {
        log.info("PDF report generation from HTML for {}", reportDetail.getLabel());
        String pdfFile = pdfHtmlFile.replace("-pdf.html", ".pdf");
        try {
          PdfHtmlFooter.manipulatePdf(pdfHtmlFile, pdfFile);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        reportDetail.getReports().put(ReportEnum.MAIN_PDF, pdfFile);
      } else {
        throw new RuntimeException(String.format("File %s does not exist.", pdfHtmlFile));
      }
    }
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(request.getPublicationDate(), "publicationDate");
  }
}
