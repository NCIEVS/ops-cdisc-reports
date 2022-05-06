package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.TextExcelReportGenerator;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.model.ThesaurusRequest;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getBaseOutputDirectory;

public class LambdaHandler implements RequestHandler<ThesaurusRequest, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public ReportSummary handleRequest(ThesaurusRequest input, Context context) {
    validate(input);
    List<ReportDetail> details = new ArrayList<>();
    for (String conceptCode : input.getConceptCodes()) {
      TextExcelReportGenerator textExcelReportGenerator =
          new TextExcelReportGenerator(new File(input.getThesaurusOwlFile()), getBaseOutputDirectory());
      ReportDetail reportDetail = textExcelReportGenerator.run(conceptCode);
      details.add(reportDetail);
    }
    return ReportSummary.builder()
        .reportDetails(details)
        .publicationDate(input.getPublicationDate())
        .build();
  }

  private void validate(ThesaurusRequest request) {
    AssertUtils.assertRequired(request, "request");
    AssertUtils.assertRequired(request.getConceptCodes(), "conceptCodes");
    AssertUtils.assertRequired(request.getPublicationDate(),"publicationDate");
    AssertUtils.assertRequired(request.getPublicationDate(),"thesaurusOwlFile");
  }
}
