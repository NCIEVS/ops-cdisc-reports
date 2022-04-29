package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.ReportContext;
import gov.nih.nci.evs.cdisc.report.ReportResponse;
import gov.nih.nci.evs.cdisc.report.TextExcelReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LambdaHandler implements RequestHandler<LambdaRequest, LambdaResponse> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public LambdaResponse handleRequest(LambdaRequest input, Context context) {
    validate(input);
    ReportContext reportContext =
        new ReportContext(
            new File(input.getThesaurusOwlFile()),
            input.getConceptCodes(),
            getOutputDirectory(),
            null);
    List<ReportResponse> reportResponseList = new TextExcelReportGenerator().run(reportContext);
    return new LambdaResponse(reportResponseList, input.getPublicationDate());
  }

  private void validate(LambdaRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("LambdaRequest is required");
    }
    if (request.getConceptCodes() == null || request.getConceptCodes().isEmpty()) {
      throw new IllegalArgumentException("conceptCodes are required");
    }
    if (request.getPublicationDate() == null || request.getPublicationDate().trim().length() == 0) {
      throw new IllegalArgumentException("publicationDate is required");
    }
    if (request.getThesaurusOwlFile() == null
        || request.getThesaurusOwlFile().trim().length() == 0) {
      throw new IllegalArgumentException("thesaurusOwlFile is required");
    }
  }

  private Path getOutputDirectory() {
    Path outputDirectory = Path.of("/mnt", "cdisc", "work");
    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create output directory", e);
    }
    return outputDirectory;
  }
}
