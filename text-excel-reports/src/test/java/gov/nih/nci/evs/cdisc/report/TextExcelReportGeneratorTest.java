package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import gov.nih.nci.evs.test.utils.AssertExcelFiles;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import static gov.nih.nci.evs.test.utils.TestUtils.getResourceFilePath;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextExcelReportGeneratorTest {

  @TempDir public File outFolder;

  @ParameterizedTest
  @CsvSource({
    "ADaM Terminology,C81222",
    "Glossary Terminology,C67497",
    "CDASH Terminology,C77527",
    "Define-XML Terminology,C165634",
    "QRS Terminology,C120166",
    "Protocol Terminology,C132298",
    "SDTM Terminology,C66830",
    "SEND Terminology,C77526"
  })
  public void testGenerate(String subSource, String subSourceCode) throws IOException {
    // The expected files for this test were generated using Thesaurus-220314-22.03b.owl file. This
    // test is only valid with that file. Not adding to this project as the file is quite large
    Path outputFolderPath = outFolder.toPath();
    TextExcelReportGenerator generator =
        new TextExcelReportGenerator(
            new File(getResourceFilePath(format("/fixtures/abridged-owl-files/%s.owl", subSource))),
            outputFolderPath);
    AssertExcelFiles assertExcelFiles = new AssertExcelFiles();
    generator.run(subSourceCode);
    String textFileName = format("%s.txt", subSource);

    File textFile =
        ReportUtils.getOutputPath(outputFolderPath, ReportUtils.getShortCodeLabel(subSource))
            .resolve(textFileName)
            .toFile();
    File excelFile = new File(textFile.getAbsolutePath().replace(".txt", ".xls"));

    assertTrue(textFile.exists());
    assertTrue(excelFile.exists());
    assertThat(textFile)
        .hasContent(
            IOUtils.toString(
                this.getClass()
                    .getResourceAsStream(
                        format("/fixtures/text-excel-from-abridged-owl/%s.txt", subSource)),
                Charset.defaultCharset()));

    assertExcelFiles.assertLegacyExcel(
        new FileInputStream(excelFile),
        this.getClass()
            .getResourceAsStream(
                format("/fixtures/text-excel-from-abridged-owl/%s.xls", subSource)));
    // We want to retain the files for troubleshooting when the test fails
    textFile.delete();
    excelFile.delete();
  }

  @ParameterizedTest
  @CsvSource({
          "ADaM Terminology,C81222",
          "Glossary Terminology,C67497",
          "CDASH Terminology,C77527",
          "Define-XML Terminology,C165634",
          "QRS Terminology,C120166",
          "Protocol Terminology,C132298",
          "SDTM Terminology,C66830",
          "SEND Terminology,C77526"
  })
  public void testGenerateV2(String subSource, String subSourceCode) throws IOException {
    // The expected files for this test were generated using Thesaurus-220314-22.03b.owl file. This
    // test is only valid with that file. Not adding to this project as the file is quite large
    Path outputFolderPath = outFolder.toPath();
    TextExcelReportGeneratorV2 generator =
            new TextExcelReportGeneratorV2(
                    new File(getResourceFilePath(format("/fixtures/abridged-owl-files/%s.owl", subSource))),
                    outputFolderPath);
    AssertExcelFiles assertExcelFiles = new AssertExcelFiles();
    generator.run(subSourceCode);
    String textFileName = format("%s.txt", subSource);

    File textFile =
            ReportUtils.getOutputPath(outputFolderPath, ReportUtils.getShortCodeLabel(subSource))
                    .resolve(textFileName)
                    .toFile();
    File excelFile = new File(textFile.getAbsolutePath().replace(".txt", ".xls"));

    assertTrue(textFile.exists());
    assertTrue(excelFile.exists());
    assertThat(textFile)
            .hasContent(
                    IOUtils.toString(
                            this.getClass()
                                    .getResourceAsStream(
                                            format("/fixtures/text-excel-from-abridged-owl/%s.txt", subSource)),
                            Charset.defaultCharset()));

    assertExcelFiles.assertLegacyExcel(
            new FileInputStream(excelFile),
            this.getClass()
                    .getResourceAsStream(
                            format("/fixtures/text-excel-from-abridged-owl/%s.xls", subSource)));
    // We want to retain the files for troubleshooting when the test fails
    textFile.delete();
    excelFile.delete();
  }
}
