package gov.nih.nci.evs.cdisc.report;

import com.google.common.collect.Sets;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import gov.nih.nci.evs.cdisc.report.utils.ZipUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class OwlZipFileGenerator {
  private Path staticFilesPath;

  public OwlZipFileGenerator(Path staticFilesPath) {
    this.staticFilesPath = staticFilesPath;
  }

  public String generateOwlZipFile(File owlFile) {
    AssertUtils.assertRequired(owlFile, "owlFile");
    AssertUtils.assertFileExists(owlFile, "owlFile");

    Set<File> files = copyStaticFiles(owlFile);
    files.add(owlFile);

    String owlZipFileName = owlFile.getAbsolutePath() + ".zip";
    ZipUtils.zipFiles(files, owlZipFileName);
    return new File(owlZipFileName).getName();
  }

  private Set<File> copyStaticFiles(File owlFile) {
    Path ctSchemaPath = staticFilesPath.resolve("ct-schema.owl");
    Path metaSchemaPath = staticFilesPath.resolve("meta-model-schema.owl");
    copyStaticFile(owlFile.getParentFile().toPath(), ctSchemaPath);
    copyStaticFile(owlFile.getParentFile().toPath(), metaSchemaPath);
    return Sets.newHashSet(ctSchemaPath.toFile(), metaSchemaPath.toFile());
  }

  private void copyStaticFile(Path conceptDirectory, Path staticFile) {
    AssertUtils.assertFileExists(staticFile.toFile(), staticFile.toFile().getName());
    Path outFilePath = conceptDirectory.resolve(staticFile.getFileName());
    try {
      Files.copy(staticFile, new FileOutputStream(outFilePath.toFile()));
    } catch (IOException e) {
      throw new RuntimeException(String.format("Error copying static file %s", staticFile), e);
    }
  }
}
