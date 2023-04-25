package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import gov.nih.nci.evs.test.utils.AssertExcelFiles;
import gov.nih.nci.evs.test.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getShortCodeLabel;
import static java.lang.String.format;

public class CDISCPairingTest {
  @TempDir public File outFolder;

  @ParameterizedTest
  @CsvSource({"ADaM Terminology,C81222", "SDTM Terminology,C66830", "SEND Terminology,C77526"})
  public void testGenerate(String subSource, String subSourceCode) throws IOException {
    CDISCPairing pairing =
        new CDISCPairing(
            new File(
                TestUtils.getResourceFilePath(
                    format("/fixtures/abridged-owl-files/%s.owl", subSource))),
            outFolder.toPath(),
            "2022-03-25");
    pairing.run(subSourceCode, CDISCPairing.DATA_SOURCE_NCIT_OWL);
    AssertExcelFiles assertExcelFiles = new AssertExcelFiles();
    assertExcelFiles.assertExcel(
        this.getClass()
            .getResourceAsStream(
                format(
                    "/fixtures/pairing-report-from-abridged-owl/%s",
                    getPairingReportFilename(subSource, null))),
        // The file needs the date to converted to all lower case to
        new FileInputStream(
            ReportUtils.getOutputPath(outFolder.toPath(), getShortCodeLabel(subSource))
                .resolve(getPairingReportFilename(subSource, null).replace("-", "_"))
                .toFile()));
  }

  @ParameterizedTest
  @CsvSource({"ADaM Terminology,C81222", "SDTM Terminology,C66830", "SEND Terminology,C77526"})
  public void testGenerateV2(String subSource, String subSourceCode) throws IOException {
    CDISCPairingV2 pairing =
        new CDISCPairingV2(
            new File(
                TestUtils.getResourceFilePath(
                    format("/fixtures/abridged-owl-files/%s.owl", subSource))),
            outFolder.toPath(),
            "2022-03-25");
    pairing.run(subSourceCode);
    AssertExcelFiles assertExcelFiles = new AssertExcelFiles();
    assertExcelFiles.assertExcel(
        this.getClass()
            .getResourceAsStream(
                format(
                    "/fixtures/pairing-report-from-abridged-owl/%s",
                    getPairingReportFilename(subSource, null))),
        // The file needs the date to converted to all lower case to
        new FileInputStream(
            ReportUtils.getOutputPath(outFolder.toPath(), getShortCodeLabel(subSource))
                .resolve(getPairingReportFilename(subSource, "v2").replace("-", "_"))
                .toFile()));
  }

  private String getPairingReportFilename(String subSource, String version) {
    return String.format(
        "%s_paired_view_2022-03-25%s.xlsx",
        getShortCodeLabel(subSource), version != null ? ("_" + version) : "");
  }

  @AfterEach
  public void cleanup() {
    new File("metadata_1.txt").delete();
    new File("pairedTermData_1.txt").delete();
    new File("readme.txt").delete();
  }
}
