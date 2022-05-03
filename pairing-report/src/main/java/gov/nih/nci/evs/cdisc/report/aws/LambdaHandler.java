package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.CDISCPairing;
import gov.nih.nci.evs.cdisc.report.ReportResponse;
import gov.nih.nci.evs.cdisc.report.model.PairingReportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LambdaHandler implements RequestHandler<LambdaRequest, String> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public String handleRequest(LambdaRequest input, Context context) {
    validate(input);
    PairingReportContext reportContext =
        PairingReportContext.builder()
            .inputFile(new File(input.getThesaurusOwlFile()))
            .rootCodes(input.getRootConceptCodes())
            .outputDirectory(getOutputDirectory())
            .publicationDate(input.getPublicationDate())
            .dataSource(CDISCPairing.DATA_SOURCE_NCIT_OWL)
            .build();
    List<ReportResponse> reportResponseList = new CDISCPairing().run(reportContext);
    return "{'status':'success'}";
  }

  private void validate(LambdaRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("LambdaRequest is required");
    }
    if (request.getRootConceptCodes() == null || request.getRootConceptCodes().isEmpty()) {
      throw new IllegalArgumentException("conceptCodes are required");
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
