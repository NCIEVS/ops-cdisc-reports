package gov.nih.nci.evs.cdisc.report.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

public class ReportUtils {
  public static final Logger log = LoggerFactory.getLogger(ReportUtils.class);

  public static Path getBaseOutputDirectory() {
    Path outputDirectory = Path.of("/mnt", "cdisc", "work", "current");
    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create output directory", e);
    }
    return outputDirectory;
  }

  public static Path getOutputPath(Path basePath, String... children) {
    assertRequired(basePath, "basePath");
    Path path = Path.of(basePath.toString());
    if (children != null) {
      for (String child : children) {
        path = path.resolve(child);
      }
    }
    createDirectories(path);
    return path;
  }

  public static void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      log.error("Unable to create directories. Path:{}", path);
      throw new RuntimeException("Exception occurred when creating directories", e);
    }
  }

  public static String getShortCodeLabel(String codeLabel) {
    AssertUtils.assertRequired(codeLabel, "codeLabel");
    return codeLabel.replace("CDISC", "").replace("Terminology", "").trim();
  }
}
