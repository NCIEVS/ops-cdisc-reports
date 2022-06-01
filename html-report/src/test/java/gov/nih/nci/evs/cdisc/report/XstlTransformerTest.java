package gov.nih.nci.evs.cdisc.report;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getShortCodeLabel;
import static gov.nih.nci.evs.test.utils.TestUtils.getResourceFilePath;
import static java.lang.String.format;

public class XstlTransformerTest {
  @TempDir public File outFolder;

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
  public void testConvertXmlToHtml(String subSource) throws IOException {
    String htmlReportFileName = outFolder.toPath().resolve(format("%s.html", subSource)).toString();
    XsltTransformer.convertXmlToHtml(
        getResourceFilePath(
            format(
                "/fixtures/report-files/%s/%s.odm.xml", getShortCodeLabel(subSource), subSource)),
        htmlReportFileName,
        "/xslt/controlledterminology1-0-0.xsl");
    File htmlReportFile = new File(htmlReportFileName);
    Assertions.assertThat(htmlReportFile).exists();
    String actualHtml =
        IOUtils.toString(new FileInputStream(htmlReportFile), Charset.defaultCharset());
    String expectedHtml =
        IOUtils.toString(
            XstlTransformerTest.class.getResourceAsStream(
                format(
                    "/fixtures/report-files/%s/%s.html", getShortCodeLabel(subSource), subSource)),
            Charset.defaultCharset());
    Assertions.assertThat(actualHtml).isEqualToIgnoringWhitespace(expectedHtml);
  }

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
  public void testConvertXmlToPdfHtml(String subSource) throws IOException {
    String htmlReportFileName = outFolder.toPath().resolve(format("%s.html", subSource)).toString();
    XsltTransformer.convertXmlToHtml(
        getResourceFilePath("/fixtures/report-files/%s/%s.odm.xml", subSource),
        htmlReportFileName,
        "/xslt/controlledterminology1-0-0-pdf.xsl");
    File htmlReportFile = new File(htmlReportFileName);
    Assertions.assertThat(htmlReportFile).exists();
    String actualHtml =
        IOUtils.toString(new FileInputStream(htmlReportFile), Charset.defaultCharset());
    String expectedHtml =
        IOUtils.toString(
            XstlTransformerTest.class.getResourceAsStream(
                format(
                    "/fixtures/report-files/%s/%s 4pdf.html",
                    getShortCodeLabel(subSource), subSource)),
            Charset.defaultCharset());
    Assertions.assertThat(actualHtml).isEqualToIgnoringWhitespace(expectedHtml);
  }
}
