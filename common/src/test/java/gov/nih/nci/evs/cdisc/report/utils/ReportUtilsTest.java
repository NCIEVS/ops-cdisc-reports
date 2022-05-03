package gov.nih.nci.evs.cdisc.report.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getOutputPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportUtilsTest {
  @TempDir public File outFolder;

  @Test
  public void testGetFile_null() {
    assertThatThrownBy(() -> getOutputPath(null, null))
        .hasMessageContaining("basePath is required")
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testGetFile_null_children() {
    Path path = getOutputPath(outFolder.toPath());
    assertThat(path).isEqualTo(outFolder.toPath());
  }

  @Test
  public void testGetFile_create_directory() throws IOException {
    Path path = getOutputPath(outFolder.toPath(), "child");
    assertThat(path.toFile()).exists();
    assertThat(path.toFile()).isDirectory();
  }
}
