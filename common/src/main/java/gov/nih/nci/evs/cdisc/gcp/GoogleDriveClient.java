package gov.nih.nci.evs.cdisc.gcp;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.common.collect.Lists;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

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
          new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credentials)
              .setApplicationName(APPLICATION_NAME)
              .build();
    } catch (IOException | GeneralSecurityException e) {
      throw new RuntimeException("Error occurred when getting credentials for GCP", e);
    }
  }

  public File getFolder(String folderName) throws IOException {
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

  public File uploadFile(java.io.File file, String parentFolderId) throws IOException {
    AssertUtils.assertRequired(file, "file");
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
    return
        drive.files().create(fileMetadata, mediaContent).setFields("id, parents").execute();
  }

  /**
   * Creates a folder in Google Drive
   * @param folderName required. Name of the folder to create
   * @param parentFolderId The parent folder Id under which this folder will be created. If null, the folder will be created as a top level folder
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
}
