package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.aws.SecretsClient;
import gov.nih.nci.evs.cdisc.gcp.GoogleDriveClient;
import gov.nih.nci.evs.cdisc.report.UploadReportsService;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    validate(input);
    GoogleDriveClient googleDriveClient =
        new GoogleDriveClient(SecretsClient.getSecret("/nci/cdisc/gdrive"));
    UploadReportsService service = new UploadReportsService(googleDriveClient);
    service.uploadReportsFolder(input.getDeliveryEmailAddresses());
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getDeliveryEmailAddresses(), "deliveryEmailAddresses");
  }
}
