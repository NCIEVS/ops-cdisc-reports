package gov.nih.nci.evs.cdisc.gcp;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.common.collect.Lists;
import gov.nih.nci.evs.cdisc.aws.SecretsClient;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GoogleDriveClient {
  private static final Logger log = LoggerFactory.getLogger(GoogleDriveClient.class);
  private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String TOKENS_DIRECTORY_PATH = "tokens";
  private static final List<String> SCOPES = Lists.newArrayList(DriveScopes.DRIVE);

  private Drive drive;

  public GoogleDriveClient(String credentialsJson) {
    try {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
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

  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
      throws IOException {
    String credentialsJson = SecretsClient.getSecret("/nci/cdisc/gdrive");
    GoogleClientSecrets clientSecrets =
        GoogleClientSecrets.load(JSON_FACTORY, new StringReader(credentialsJson));

    // Build flow and trigger user authorization request.
    GoogleAuthorizationCodeFlow flow =
        new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential =
        new AuthorizationCodeInstalledApp(flow, receiver).authorize("WCI GDrive User");
    // returns an authorized Credential object.
    return credential;
  }

  public static File getFolder(Drive drive, String folderName) throws IOException {
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

  public File uploadFile(File folder, java.io.File file) throws IOException {
    File fileMetadata = new File();
    fileMetadata.setName(file.getName());
    fileMetadata.setParents(Lists.newArrayList(folder.getId()));
    FileContent mediaContent = null;
    try {
      mediaContent = new FileContent(Files.probeContentType(file.toPath()), file);
    } catch (IOException e) {
      mediaContent = new FileContent("application/octet-stream", file);
    }
    File uploadedFile =
        drive.files().create(fileMetadata, mediaContent).setFields("id, parents").execute();
    return uploadedFile;
  }

  @SneakyThrows
  public File createTargetFolder(String targetFolder, String parentFolderId) {
    File folderMetadata = new File();
    folderMetadata.setName(targetFolder);
    folderMetadata.setMimeType("application/vnd.google-apps.folder");
    if (StringUtils.isNotBlank(parentFolderId)) {
      folderMetadata.setParents(Collections.singletonList(parentFolderId));
    }
    return drive.files().create(folderMetadata).setFields("id, webViewLink").execute();
  }

  @SneakyThrows
  public void grantWritePermissions(File targetFolder, List<String> emailAddresses) {
    AssertUtils.assertRequired(targetFolder, "targetFolder");
    AssertUtils.assertRequired(emailAddresses, "emailAddresses");
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
