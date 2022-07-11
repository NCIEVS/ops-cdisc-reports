package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.CDISCPairing;
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

  /**
   * Generates pairing report Excel file from a given Thesaurus file
   * @param input path to Thesaurus file
   * @param context lambda context
   * @return augmented report list with the pairing report files
   */
  @Override
  public ReportSummary handleRequest(ThesaurusRequest input, Context context) {
    validate(input);
    List<ReportDetail> details = new ArrayList<>();
    for (String code : input.getConceptCodes()) {
      ReportDetail reportDetail =
          new CDISCPairing(
                  new File(input.getThesaurusOwlFile()),
                  getBaseOutputDirectory(),
                  input.getPublicationDate())
              .run(code, CDISCPairing.DATA_SOURCE_NCIT_OWL);
      details.add(reportDetail);
    }
    return ReportSummary.builder()
            .reportDetails(details)
            .publicationDate(input.getPublicationDate())
            .deliveryEmailAddresses(input.getDeliveryEmailAddresses())
            .build();
  }

  private void validate(ThesaurusRequest request) {
    AssertUtils.assertRequired(request.getConceptCodes(), "conceptCodes");
    AssertUtils.assertRequired(request.getThesaurusOwlFile(), "thesaurusOwlFile");
  }
}
