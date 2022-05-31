package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.test.utils.TestUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;

import static java.lang.String.format;

public class HtmlToPdfConverterTest {

  @TempDir
  public File outFolder;

  @ParameterizedTest
  @CsvSource({
    "ADaM Terminology",
    "Glossary Terminology",
    "CDASH Terminology",
    "Define-XML Terminology",
    "Protocol Terminology",
    "SDTM Terminology",
    "SEND Terminology"
  })
  public void testConvert(String subSource) throws IOException {
    String pdfReportFileName = outFolder.toPath().resolve(format("%s.pdf", subSource)).toString();
    HtmlToPdfConverter.convert(
        TestUtils.getResourceFilePath("/fixtures/report-files/%s/%s 4pdf.html", subSource),pdfReportFileName);
    Assertions.assertThat(new File(pdfReportFileName)).exists();
  }
}
