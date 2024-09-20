package gov.nih.nci.evs.cdisc.gcp;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client that facilitates calls to Google Drive */
public class GoogleDriveClient {
  private static final Logger log = LoggerFactory.getLogger(GoogleDriveClient.class);
  private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final List<String> SCOPES = Lists.newArrayList(DriveScopes.DRIVE);

  private final Drive drive;

  public GoogleDriveClient(String credentialsJson) {
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      /**
       * The sample from Google Drive API documentation uses this deprecated class. I tried to use
       * the GoogleCredentials class from google-auth-library-oauth2-http. But that class is
       * incompatible with Drive class. So using this deprecated class for now.
       */
      GoogleCredential credentials =
          GoogleCredential.fromStream(
                  IOUtils.toInputStream(credentialsJson, Charset.defaultCharset()))
              .createScoped(SCOPES);
      this.drive =
          new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, setHttpTimeout(credentials))
              .setApplicationName(APPLICATION_NAME)
              .build();
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException("Error occurred when getting credentials for GCP", e);
    }
  }

  /**
   * Get Google Drive Folder
   *
   * @param folderName required, name of the folder
   * @return folder if found else null
   * @throws IOException
   */
  public File getFolder(String folderName) throws IOException {
    assertRequired(folderName, "folderName");
    FileList result =
        drive
            .files()
            .list()
            .setQ(
                String.format(
                    "mimeType='application/vnd.google-apps.folder' and name='%s'", folderName))
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute();
    return result.getFiles().isEmpty() ? null : result.getFiles().get(0);
  }

  /**
   * Upload a file to Google drive
   *
   * @param file required, file to upload
   * @param parentFolderId optional, Google Drive folder id of the folder which would contain the
   *     file
   * @return uploaded Google file
   * @throws IOException
   */
  public File uploadFile(java.io.File file, String parentFolderId) throws IOException {
    assertRequired(file, "file");
    File fileMetadata = new File();
    fileMetadata.setName(file.getName());
    if (StringUtils.isNotBlank(parentFolderId)) {
      fileMetadata.setParents(Lists.newArrayList(parentFolderId));
    }
    FileContent mediaContent;
    try {
      mediaContent = new FileContent(Files.probeContentType(file.toPath()), file);
    } catch (IOException e) {
      mediaContent = new FileContent("application/octet-stream", file);
    }
    return drive.files().create(fileMetadata, mediaContent).setFields("id, parents").execute();
  }

  /**
   * Creates a folder in Google Drive
   *
   * @param folderName required. Name of the folder to create
   * @param parentFolderId The parent folder Id under which this folder will be created. If null,
   *     the folder will be created as a top level folder
   * @param fields Comma separated fields that need to be returned in the response
   * @return metadata of the folder that was created
   */
  @SneakyThrows
  public File createFolder(String folderName, String parentFolderId, String fields) {
    assertRequired(folderName, "folderName");
    File folderMetadata = new File();
    folderMetadata.setName(folderName);
    folderMetadata.setMimeType("application/vnd.google-apps.folder");
    if (StringUtils.isNotBlank(parentFolderId)) {
      folderMetadata.setParents(Collections.singletonList(parentFolderId));
    }
    return drive.files().create(folderMetadata).setFields(fields).execute();
  }

  /**
   * Grants a set of email address write permissions to a Google Drive folder
   *
   * @param targetFolder required, Google Drive folder to which write permissions need to be granted
   * @param emailAddresses required, list of email addresses to which write permissions need to be
   *     granted
   */
  @SneakyThrows
  public void grantWritePermissions(File targetFolder, List<String> emailAddresses) {
    assertRequired(targetFolder, "targetFolder");
    assertRequired(emailAddresses, "emailAddresses");
    for (String emailAddress : emailAddresses) {
      drive
          .permissions()
          .create(
              targetFolder.getId(),
              new Permission().setEmailAddress(emailAddress).setType("user").setRole("writer"))
          .execute();
    }
  }

  /**
   * The folders created to host the reports are not useful after the files have been transferred to
   * the CDISC SFTP site. This method deletes these old folders.
   *
   * @param thresholdInDays Retain folders new that this. May want to keep the most recent folder
   */
  @SneakyThrows
  public void deleteOldFolders(int thresholdInDays) {
    FileList result =
        drive
            .files()
            .list()
            .setQ("mimeType='application/vnd.google-apps.folder'")
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute();
    if (thresholdInDays > 0) {
      List<File> oldReportFolders =
          result.getFiles().stream()
              .filter(folder -> filterFolderByDate(folder, thresholdInDays))
              .collect(Collectors.toList());
      log.info("Deleting {} directories", oldReportFolders.size());
      for (File oldReportFolder : oldReportFolders) {
        log.info("Deleting {}", oldReportFolder.getName());
        drive.files().delete(oldReportFolder.getId()).execute();
      }
    } else {
      for(File file : result.getFiles()){
        log.info("Deleting {}", file.getName());
        drive.files().delete(file.getId()).execute();
      }
    }
  }

  private boolean filterFolderByDate(File folder, int thresholdInDays) {
    LocalDate now = LocalDate.now();
    DateTimeFormatter folderPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    if (folder.getName().length() > 11) {
      try {
        return LocalDate.parse(folder.getName().substring(0, 10), folderPattern)
            .isBefore(now.minusDays(thresholdInDays));
      } catch (DateTimeParseException dtpe) {
        // Ignore any parse exceptions. Those are non-report folders
        log.debug("Not a report folder. Folder Name:{}", folder.getName());
      }
    }
    return false;
  }

  private HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
    return new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        requestInitializer.initialize(httpRequest);
        httpRequest.setConnectTimeout(15 * 60000); // 3 minutes connect timeout
        httpRequest.setReadTimeout(15 * 60000); // 3 minutes read timeout
      }
    };
  }
}
