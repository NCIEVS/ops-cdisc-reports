package gov.nih.nci.evs.cdisc.report;

import com.google.api.services.drive.model.File;
import com.google.common.collect.Lists;
import gov.nih.nci.evs.cdisc.aws.SecretsClient;
import gov.nih.nci.evs.cdisc.gcp.GoogleDriveClient;
import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UploadReportsService {
  private final static Logger log = LoggerFactory.getLogger(UploadReportsService.class);

  private final GoogleDriveClient googleDriveClient;

  // SecretsClient.getSecret("/nci/cdisc/gdrive");
  public UploadReportsService(GoogleDriveClient googleDriveClient) {
    this.googleDriveClient = googleDriveClient;
  }

  public void uploadReportsFolder(List<String> emailAddresses) {
    Path sourceFolder = ReportUtils.getBaseOutputDirectory();
    uploadReportsFolder(emailAddresses, sourceFolder);
  }

  public void uploadReportsFolder(List<String> emailAddresses, Path sourceFolder) {
    String targetFolder =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));
    com.google.api.services.drive.model.File driveTargetFolder =
        googleDriveClient.createTargetFolder(targetFolder, null);
    log.info("Report Upload Folder Id:{} ", driveTargetFolder.getId());
    googleDriveClient.grantWritePermissions(driveTargetFolder, emailAddresses);
    uploadFolder(sourceFolder.toFile(), driveTargetFolder);
  }

  private void uploadFolder(java.io.File folder, File parentFolder) {
    log.info("Uploading folder {}", folder.getName());
    if (folder.listFiles() != null) {
      for (java.io.File file : folder.listFiles()) {
        if (file.isDirectory()) {
          log.info("Found sub directory {}. Creating.", file.getName());
          File googleDriveFolder =
              googleDriveClient.createTargetFolder(file.getName(), parentFolder.getId());
          uploadFolder(file, googleDriveFolder);
        } else {
          try {
            log.debug(
                "Uploading file: {}. Parent folder:{}", file.getName(), parentFolder.getName());
            googleDriveClient.uploadFile(parentFolder, file);
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

  public static void main(String[] args) {
    GoogleDriveClient googleDriveClient =
        new GoogleDriveClient(SecretsClient.getSecret("/nci/cdisc/gdrive"));
    UploadReportsService service = new UploadReportsService(googleDriveClient);
    service.uploadReportsFolder(
        Lists.newArrayList("akuppusamy@westcoastinformatics.com"),
        Path.of("/Users/squareroot/temp/Current_6"));
  }
}
