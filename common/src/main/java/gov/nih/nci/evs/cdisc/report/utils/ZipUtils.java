package gov.nih.nci.evs.cdisc.report.utils;

import java.io.*;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
  /**
   * Compress given files and create a zip archive file
   *
   * @param srcFiles list files to compress
   * @param zipFileName output zip archive file name
   */
  public static void zipFiles(Set<File> srcFiles, String zipFileName) {
    try {
      FileOutputStream fos = new FileOutputStream(zipFileName);
      ZipOutputStream zipOut = new ZipOutputStream(fos);
      for (File srcFile : srcFiles) {
        try {
          FileInputStream fis = new FileInputStream(srcFile);
          ZipEntry zipEntry = new ZipEntry(srcFile.getName());
          zipOut.putNextEntry(zipEntry);

          byte[] bytes = new byte[1024];
          int length;
          while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
          fis.close();
        } catch (IOException ioe) {
          throw new RuntimeException(
              String.format("Error occurred while zipping %s. Zip File:%s", srcFile, zipFileName),
              ioe);
        }
      }
      zipOut.close();
      fos.close();
    } catch (IOException ioe) {
      throw new RuntimeException(
          String.format("Error occurred while creating zip file %s", zipFileName), ioe);
    }
  }

  /**
   * Decompress files from a zip archive
   *
   * @param zipFile zip archive file
   * @param outputDirectory directory to which the files will get decompressed and extracted to
   */
  public static void unzipFiles(File zipFile, Path outputDirectory) {
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(new FileInputStream(zipFile));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(String.format("Zip file %s not found", zipFile), e);
    }
    ZipEntry zipEntry = null;
    try {
      zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        File newFile = newFile(outputDirectory.toFile(), zipEntry);
        if (zipEntry.isDirectory()) {
          if (!newFile.isDirectory() && !newFile.mkdirs()) {
            throw new IOException("Failed to create directory " + newFile);
          }
        } else {
          // fix for Windows-created archives
          File parent = newFile.getParentFile();
          if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory " + parent);
          }

          // write file content
          FileOutputStream fos = new FileOutputStream(newFile);
          int len;
          byte[] buffer = new byte[1024];
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
    } catch (IOException e) {
      throw new RuntimeException(String.format("Error occurred when unzipping %s", zipFile), e);
    }
  }

  /**
   * Makes sure decompressed files are created within the given output directory. This is to avoid
   * <a href="https://snyk.io/research/zip-slip-vulnerability">Zip Slip</a>
   *
   * @param destinationDir directory to extract files to
   * @param zipEntry compressed file in the zip archive
   * @return a logical new file inside the destinationDir
   * @throws IOException
   */
  public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destFile = new File(destinationDir, zipEntry.getName());

    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    return destFile;
  }
}
