package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import gov.nih.nci.evs.cdisc.report.utils.ZipUtils;
import gov.nih.nci.evs.test.utils.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class OwlZipFileGeneratorTest {
  private static final File OWL_FILE =
      new File(TestUtils.getResourceFilePath("/fixtures/report-files/ADaM/ADaM Terminology.owl"));
  private OwlZipFileGenerator owlZipFileGenerator =
      new OwlZipFileGenerator(
          Path.of(TestUtils.getResourceFilePath("/fixtures/report-files/static-files")));

  @TempDir public File outFolder;

  @Test
  public void testGenerateOwlZipFile_null() {
    Assertions.assertThatThrownBy(() -> owlZipFileGenerator.generateOwlZipFile(null))
        .hasMessage("owlFile is required");
  }

  @Test
  public void testGenerateOwlZipFile_owl_file_does_not_exist() {
    Assertions.assertThatThrownBy(
            () -> owlZipFileGenerator.generateOwlZipFile(new File("/test.owl")))
        .hasMessage("owlFile failed precondition");
  }

  @Test
  public void testGenerateOwlZipFile_static_file_does_not_exist() {
    owlZipFileGenerator = new OwlZipFileGenerator(Path.of("does-not-exist"));
    Assertions.assertThatThrownBy(() -> owlZipFileGenerator.generateOwlZipFile(OWL_FILE))
        .hasMessage("ct-schema.owl failed precondition");
  }

  @Test
  public void testGenerateOwlZipFile_success() throws IOException {
    // Thw zip file gets created in the same directory as the OWL file. So copy the OWL file to the
    // temp test directory. This way the zip file would not be created in the
    Path tempOwlFilePath = outFolder.toPath().resolve(OWL_FILE.getName());
    Files.copy(OWL_FILE.toPath(), tempOwlFilePath);

    String zipFile = owlZipFileGenerator.generateOwlZipFile(tempOwlFilePath.toFile());
    Path zipFilePath = outFolder.toPath().resolve(zipFile);
    assertThat(zipFilePath.toFile()).exists();

    Path unzipedFilesFolder = outFolder.toPath().resolve("unzip-files");
    ReportUtils.createDirectories(unzipedFilesFolder);
    ZipUtils.unzipFiles(zipFilePath.toFile(), unzipedFilesFolder);
    assertThat(unzipedFilesFolder.toFile().listFiles())
        .hasSize(3)
        .containsExactlyInAnyOrder(
            toFile(unzipedFilesFolder, "meta-model-schema.owl"),
            toFile(unzipedFilesFolder, "ct-schema.owl"),
            toFile(unzipedFilesFolder, "ADaM Terminology.owl"));
  }

  private File toFile(Path unzipedFilesFolder, String fileName) {
    return unzipedFilesFolder.resolve(fileName).toFile();
  }
}
