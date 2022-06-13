package gov.nih.nci.evs.cdisc.report;

import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.test.Fixtures;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static gov.nih.nci.evs.cdisc.report.PostProcessService.getArchiveFileName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PostProcessServiceTest {
  @TempDir public File outFolder;

  private PostProcessService postProcessService = new PostProcessService();

  @Test
  public void testGetReportSummary_null() {
    assertThatThrownBy(() -> postProcessService.getReportSummary(null))
        .hasMessage("input is required")
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testGetReportSummary_success() throws IOException {
    List input =
        new ObjectMapper()
            .readValue(
                this.getClass()
                    .getResourceAsStream("/fixtures/lambda-request/step-function-request.json"),
                List.class);
    ReportSummary reportSummary = postProcessService.getReportSummary(input);
    assertThat(reportSummary.getPublicationDate()).isEqualTo("04-25-2022");
    assertThat(reportSummary.getDeliveryEmailAddresses()).containsExactly("test@test.com");
    assertThat(reportSummary.getReportDetails())
        .extracting("code")
        .containsExactlyInAnyOrder("C81222", "C66830", "C77526");
    for (ReportDetail detail : reportSummary.getReportDetails()) {
      Map<ReportEnum, String> reports = detail.getReports();
      assertThat(reports.keySet()).containsExactlyInAnyOrder(ReportEnum.values());
    }
  }

  @Test
  public void testArchiveFiles_null() {
    assertThatThrownBy(() -> postProcessService.archiveFiles(null))
        .hasMessage("reportSummary is required")
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testArchiveFiles_null_report_details() {
    ReportSummary reportSummary = Fixtures.getReportSummary();
    assertThatThrownBy(() -> postProcessService.archiveFiles(reportSummary))
        .hasMessage("reportDetails is required")
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testArchiveFiles_null_publication_date() {
    ReportSummary reportSummary = Fixtures.getReportSummary();
    reportSummary.setReportDetails(ImmutableList.of(new ReportDetail()));
    reportSummary.setPublicationDate(null);
    assertThatThrownBy(() -> postProcessService.archiveFiles(reportSummary))
        .hasMessage("publicationDate is required")
        .isExactlyInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testArchiveFiles_success() throws IOException {
    ReportSummary reportSummary = Fixtures.getReportSummary();
    reportSummary.setReportDetails(getReportDetails());
    postProcessService.archiveFiles(reportSummary);
    Assertions.assertThat(getArchiveFilePath("test.txt")).hasContent("test text");
    Assertions.assertThat(getArchiveFilePath("test.html")).hasContent("<html/>");
  }

  private List<ReportDetail> getReportDetails() throws IOException {
    String textFile = outFolder.toPath().resolve("test.txt").toString();
    IOUtils.write("test text", new FileOutputStream(textFile), Charset.defaultCharset());
    String htmlFile = outFolder.toPath().resolve("test.html").toString();
    IOUtils.write("<html/>", new FileOutputStream(htmlFile), Charset.defaultCharset());
    return ImmutableList.of(
        ReportDetail.builder()
            .reports(
                ImmutableMap.of(ReportEnum.MAIN_TEXT, textFile, ReportEnum.MAIN_HTML, htmlFile))
            .build());
  }

  private Path getArchiveFilePath(String file) {
    return outFolder
        .toPath()
        .resolve(PostProcessService.ARCHIVE_DIRECTORY)
        .resolve(getArchiveFileName(file, Fixtures.PUBLICATION_DATE));
  }
}
