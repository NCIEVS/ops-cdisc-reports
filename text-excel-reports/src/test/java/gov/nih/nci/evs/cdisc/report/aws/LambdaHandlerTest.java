package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import gov.nih.nci.evs.cdisc.report.TextExcelReportGenerator;
import gov.nih.nci.evs.cdisc.report.TextExcelReportGeneratorFactory;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.model.ThesaurusRequest;
import gov.nih.nci.evs.test.Fixtures;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LambdaHandlerTest {
  @Mock private Context mockContext;
  @Mock private TextExcelReportGeneratorFactory mockFactory;

  @InjectMocks private LambdaHandler handler = new LambdaHandler();

  @Test
  public void testHandleRequest_no_owl_file() {
    ThesaurusRequest request = Fixtures.getRequest();
    request.setThesaurusOwlFile(null);
    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> handler.handleRequest(request, mockContext))
        .withMessage("thesaurusOwlFile is required");
  }

  @Test
  public void testHandleRequest_no_publication_date() {
    ThesaurusRequest request = Fixtures.getRequest();
    request.setPublicationDate(null);
    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> handler.handleRequest(request, mockContext))
        .withMessage("publicationDate is required");
  }

  @Test
  public void testHandleRequest_no_concept_codes() {
    ThesaurusRequest request = Fixtures.getRequest();
    request.setConceptCodes(null);
    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> handler.handleRequest(request, mockContext))
        .withMessage("conceptCodes is required");
  }

  @Test
  public void testHandleRequest_empty_concept_codes() {
    ThesaurusRequest request = Fixtures.getRequest();
    request.setConceptCodes(new ArrayList<>());
    Assertions.assertThatIllegalArgumentException()
        .isThrownBy(() -> handler.handleRequest(request, mockContext))
        .withMessage("conceptCodes is required");
  }

  @Test
  public void testHandleRequest_success() {
    ThesaurusRequest request = Fixtures.getRequest();
    TextExcelReportGenerator mockGenerator = Mockito.mock(TextExcelReportGenerator.class);
    when(mockFactory.createTextExcelReportGenerator(any())).thenReturn(mockGenerator);
    when(mockGenerator.run(any()))
        .then(invocation -> ReportDetail.builder().code(invocation.getArgument(0)).build());
    ReportSummary reportSummary = handler.handleRequest(request, mockContext);
    assertThat(reportSummary.getDeliveryEmailAddresses())
        .containsExactlyElementsOf(Fixtures.DELIVERY_EMAIL_ADDRESSES);
    assertThat(reportSummary.getPublicationDate()).isEqualTo(Fixtures.PUBLICATION_DATE);
    assertThat(reportSummary.getReportDetails())
        .extracting("code")
        .containsExactly(Fixtures.CONCEPT_CODE_1, Fixtures.CONCEPT_CODE_2);
  }
}
