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

  /**
   * Uploads all the reports in the output folder to a folder in Google Drive. This lambda can also
   * be used to delete Google Drive folders that are older than a particular number of days
   *
   * @param input all the reports that were generated. If deleting old reports, then the number days to keep reports for.
   * @param context lambda context
   * @return passes the input object through. No changes made.
   */
  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    GoogleDriveClient googleDriveClient =
        new GoogleDriveClient(SecretsClient.getSecret("/nci/cdisc/gdrive"));
    UploadReportsService service = new UploadReportsService(googleDriveClient);
    if (input.getDeleteOldReportsThresholdDays() == null) {
      validate(input);
      service.uploadReportsFolder(input.getDeliveryEmailAddresses());
      return input;
    } else {
      service.deleteOldReports(input.getDeleteOldReportsThresholdDays());
      return input;
    }
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getDeliveryEmailAddresses(), "deliveryEmailAddresses");
  }
}
