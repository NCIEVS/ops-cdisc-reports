package gov.nih.nci.evs.cdisc.report;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChangesReportTest {
  @TempDir public File outFolder;

  @ParameterizedTest
  @CsvSource({"ADaM", "Glossary", "CDASH", "Define-XML", "Protocol", "SDTM", "SEND"})
  public void testGenerate(String subSource) throws IOException {
    ChangesReport changesReport = new ChangesReport();
    String actualChangesReportFileName =
        outFolder.toPath().resolve(format("%s-Changes.txt", subSource)).toString();
    changesReport.init(
        getReportFileName(subSource, true),
        getReportFileName(subSource, false),
        "2022-03-25",
        actualChangesReportFileName);
    changesReport.getChanges();
    changesReport.print();

    File actualChangesReportFile = new File(actualChangesReportFileName);
    File expectedChangesReportFile =
        new File(
            getClass().getResource(format("/fixtures/changes-report/%s.txt", subSource)).getPath());
    assertTrue(new File(actualChangesReportFileName).exists());
    Assertions.assertThat(actualChangesReportFile).exists();
    Assertions.assertThat(actualChangesReportFile)
        .hasContent(
            IOUtils.toString(
                new FileInputStream(expectedChangesReportFile), Charset.defaultCharset()));
  }

  @ParameterizedTest
  @CsvSource({"ADaM", "Glossary", "CDASH", "Define-XML", "Protocol", "SDTM", "SEND"})
  public void testGenerateV2(String subSource) throws IOException {
    String actualChangesReportFileName =
        outFolder.toPath().resolve(format("%s-Changes_v2.txt", subSource)).toString();
    ChangesReportV2 changesReport =
        new ChangesReportV2(
            getReportFileName(subSource, true),
            getReportFileName(subSource, false),
            "2022-03-25",
            actualChangesReportFileName);
    changesReport.getChanges();
    changesReport.print();

    File actualChangesReportFile = new File(actualChangesReportFileName);
    File expectedChangesReportFile =
        new File(
            getClass().getResource(format("/fixtures/changes-report/%s.txt", subSource)).getPath());
    assertTrue(new File(actualChangesReportFileName).exists());
    Assertions.assertThat(actualChangesReportFile).exists();
    Assertions.assertThat(actualChangesReportFile)
        .hasContent(
            IOUtils.toString(
                new FileInputStream(expectedChangesReportFile), Charset.defaultCharset()));
  }

  @Test
  public void testUncommonChanges() throws IOException {
    String subSource = "Misc";
    ChangesReport changesReport = new ChangesReport();
    String actualChangesReportFileName =
        outFolder.toPath().resolve(format("%s-Changes.txt", subSource)).toString();
    changesReport.init(
        getReportFileName(subSource, true),
        getReportFileName(subSource, false),
        "2022-03-25",
        actualChangesReportFileName);
    changesReport.getChanges();
    changesReport.print();

    File actualChangesReportFile = new File(actualChangesReportFileName);
    File expectedChangesReportFile =
        new File(
            getClass().getResource(format("/fixtures/changes-report/%s.txt", subSource)).getPath());
    assertTrue(new File(actualChangesReportFileName).exists());
    Assertions.assertThat(actualChangesReportFile).exists();
    Assertions.assertThat(actualChangesReportFile)
        .hasContent(
            IOUtils.toString(
                new FileInputStream(expectedChangesReportFile), Charset.defaultCharset()));
  }

  @Test
  public void testUncommonChangesV2() throws IOException {
    String subSource = "Misc";
    String actualChangesReportFileName =
        outFolder.toPath().resolve(format("%s-Changes_v2.txt", subSource)).toString();
    ChangesReportV2 changesReport =
        new ChangesReportV2(
            getReportFileName(subSource, true),
            getReportFileName(subSource, false),
            "2022-03-25",
            actualChangesReportFileName);
    changesReport.getChanges();
    changesReport.print();

    File actualChangesReportFile = new File(actualChangesReportFileName);
    File expectedChangesReportFile =
        new File(
            getClass().getResource(format("/fixtures/changes-report/%s.txt", subSource)).getPath());
    assertTrue(new File(actualChangesReportFileName).exists());
    Assertions.assertThat(actualChangesReportFile).exists();
    Assertions.assertThat(actualChangesReportFile)
        .hasContent(
            IOUtils.toString(
                new FileInputStream(expectedChangesReportFile), Charset.defaultCharset()));
  }

  private String getReportFileName(String subSource, boolean current) {
    String subDirectory = current ? "current" : "previous";
    String reportFileName = format("/fixtures/text-report/%s/%s.txt", subDirectory, subSource);
    return getClass().getResource(reportFileName).getPath();
  }
}
