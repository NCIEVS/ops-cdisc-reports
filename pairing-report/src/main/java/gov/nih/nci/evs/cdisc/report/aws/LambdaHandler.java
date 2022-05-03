package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.CDISCPairing;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ThesaurusRequest;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getBaseOutputDirectory;

public class LambdaHandler implements RequestHandler<ThesaurusRequest, String> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public String handleRequest(ThesaurusRequest input, Context context) {
    validate(input);
    for (String code : input.getConceptCodes()) {
      ReportDetail reportResponseList =
          new CDISCPairing(
                  new File(input.getThesaurusOwlFile()),
                  getBaseOutputDirectory(),
                  input.getPublicationDate())
              .run(code, CDISCPairing.DATA_SOURCE_NCIT_OWL);
    }
    return "{'status':'success'}";
  }

  private void validate(ThesaurusRequest request) {
    AssertUtils.assertRequired(request.getConceptCodes(), "conceptCodes");
    AssertUtils.assertRequired(request.getThesaurusOwlFile(), "thesaurusOwlFile");
  }
}
