package gov.nih.nci.evs.cdisc.report.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

public class ReportUtils {
  public static final Logger log = LoggerFactory.getLogger(ReportUtils.class);

  public static Path getOutputPath(Path basePath, String... children) {
    assertRequired(basePath, "basePath");
    Path path = Path.of(basePath.toString());
    try {
      if (children != null) {
        for(String child : children){
          path = path.resolve(child);
        }
      }
      Files.createDirectories(path);
    } catch (IOException e) {
      log.error("Unable to create directories. Path:{}", path);
      throw new RuntimeException("Exception occurred when creating directories", e);
    }
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
}
