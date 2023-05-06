package gov.nih.nci.evs.cdisc.report.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;
import static org.apache.commons.lang3.StringUtils.stripAll;

public class ReportUtils {
  public static final Logger log = LoggerFactory.getLogger(ReportUtils.class);

  /**
   * Returns the directory where the reports of all the concepts are stored
   *
   * @return path to the output directory
   */
  public static Path getBaseOutputDirectory() {
    Path outputDirectory = Path.of("/mnt", "cdisc", "work", "current");
    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create output directory", e);
    }
    return outputDirectory;
  }

  /**
   * Resolves the path to the output directory. Creates the all the child directories to do not
   * currently exist
   *
   * @param basePath base output path under which the report directories for concept codes will get
   *     created
   * @param children subdirectories under the basePath
   * @return path to the fully resolved output directory
   */
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

  /**
   * Utility method that creates all directories that do not exist in the given path. Also handles
   * errors from directory creation
   *
   * @param path path to create directories from
   */
  public static void createDirectories(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      log.error("Unable to create directories. Path:{}", path);
      throw new RuntimeException("Exception occurred when creating directories", e);
    }
  }

  /**
   * Utility method to strip known strings from the full code label and just retain the label
   * relevant to the concept code
   *
   * @param codeLabel full code label
   * @return short label used to identify the concept code
   */
  public static String getShortCodeLabel(String codeLabel) {
    AssertUtils.assertRequired(codeLabel, "codeLabel");
    return codeLabel.replace("CDISC", "").replace("Terminology", "").trim();
  }

  /**
   * There are certain informational files that are included with the set of dynamically created
   * reports.
   *
   * @return directory containing static files to be included with the reports
   */
  public static Path getStaticFilesPath() {
    return Path.of("/mnt", "cdisc", "work", "static-files");
  }

  /**
   * If there are multiple synonyms in a concept, they are concatenated with a semicolon to fit into
   * one field in the text file. This method, converts that delimited string into an array of
   * synonyms
   *
   * @param strSynonyms
   * @return
   */
  public static String[] getSynonyms(String strSynonyms) {
    if (strSynonyms != null) {
      String[] tokens = strSynonyms.split(";");
      return stripAll(tokens);
    }
    return new String[] {};
  }
}
