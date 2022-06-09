package gov.nih.nci.evs.cdisc.report;

import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PostProcessServiceTest {
  private PostProcessService postProcessService;

  @Test
  public void testGetReportSummary_null() {
    Assertions.assertThatThrownBy(() -> postProcessService.getReportSummary(null))
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
}
