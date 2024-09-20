package gov.nih.nci.evs.cdisc.report;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

import com.google.api.services.drive.model.File;
import gov.nih.nci.evs.cdisc.aws.SecretsClient;
import gov.nih.nci.evs.cdisc.gcp.GoogleDriveClient;
import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadReportsService {
  private static final Logger log = LoggerFactory.getLogger(UploadReportsService.class);

  private final GoogleDriveClient googleDriveClient;

  public UploadReportsService(GoogleDriveClient googleDriveClient) {
    this.googleDriveClient = googleDriveClient;
  }

  /**
   * Upload reports in the default output folder to Google Drive
   *
   * @param emailAddresses required, list email addresses that are given write permissions to the
   *     Google Drive folder
   */
  public void uploadReportsFolder(List<String> emailAddresses) {
    assertRequired(emailAddresses, "emailAddresses");
    Path sourceFolder = ReportUtils.getBaseOutputDirectory();
    uploadReportsFolder(emailAddresses, sourceFolder);
  }

  /**
   * Upload a given folder to Google Drive
   *
   * @param emailAddresses required, list email addresses that are given write permissions to the
   *     Google Drive folder
   * @param sourceFolder required, folder containing the reports
   */
  public void uploadReportsFolder(List<String> emailAddresses, Path sourceFolder) {
    assertRequired(emailAddresses, "emailAddresses");
    assertRequired(sourceFolder, "sourceFolder");
    String targetFolder =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
    com.google.api.services.drive.model.File driveTargetFolder =
        googleDriveClient.createFolder(targetFolder, null, "id, webViewLink");
    log.info("Report Upload Folder Id:{} ", driveTargetFolder.getId());
    uploadFolder(sourceFolder.toFile(), driveTargetFolder);
    googleDriveClient.grantWritePermissions(driveTargetFolder, emailAddresses);
  }

  private void uploadFolder(java.io.File folder, File parentFolder) {
    log.info("Uploading folder {}", folder.getName());
    if (folder.listFiles() != null) {
      for (java.io.File file : Objects.requireNonNull(folder.listFiles())) {
        if (file.isDirectory()) {
          log.info("Found sub directory {}. Creating.", file.getName());
          File googleDriveFolder =
              googleDriveClient.createFolder(
                  file.getName(), parentFolder.getId(), "id, webViewLink");
          uploadFolder(file, googleDriveFolder);
        } else {
          try {
            log.debug(
                "Uploading file: {}. Parent folder:{}", file.getName(), parentFolder.getName());
            googleDriveClient.uploadFile(file, parentFolder.getId());
          } catch (IOException e) {
            throw new RuntimeException(
                String.format(
                    "Exception occurred when loading file. File:%s; Parent Folder:%s",
                    file.getName(), parentFolder.getName()),
                e);
          }
        }
      }
    }
  }

  /**
   * Deletes folders that were created older than the days specified
   *
   * @param deleteOldReportsThresholdDays folders older than this would get deleted
   */
  public void deleteOldReports(Integer deleteOldReportsThresholdDays) {
    assertRequired(deleteOldReportsThresholdDays, "deleteOldReportsThresholdDays");
    googleDriveClient.deleteOldFolders(deleteOldReportsThresholdDays);
  }

  public static void main(String[] args){
    GoogleDriveClient googleDriveClient =
            new GoogleDriveClient(SecretsClient.getSecret("/nci/cdisc/gdrive"));
    UploadReportsService service = new UploadReportsService(googleDriveClient);
    service.deleteOldReports(0);
  }
}
