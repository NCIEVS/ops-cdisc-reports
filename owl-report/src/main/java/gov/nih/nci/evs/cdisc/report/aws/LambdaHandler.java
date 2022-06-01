package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.RDFGenerator;
import gov.nih.nci.evs.cdisc.report.ReportEnum;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    validate(input);
    for (ReportDetail reportDetail : input.getReportDetails()) {
      String textFile = reportDetail.getReports().get(ReportEnum.MAIN_TEXT);
      if (textFile != null && new File(textFile).exists()) {
        String baseName = FilenameUtils.removeExtension(textFile);
        String owlFile = baseName + ".owl";

        RDFGenerator rdfGenerator = new RDFGenerator();
        rdfGenerator.generate(textFile, owlFile);
        reportDetail.getReports().put(ReportEnum.MAIN_OWL, owlFile);
      } else {
        throw new RuntimeException(String.format("File %s does not exist.", textFile));
      }
    }
    // The Excel file report gets formatted in place, so
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(request.getPublicationDate(), "publicationDate");
  }
}
