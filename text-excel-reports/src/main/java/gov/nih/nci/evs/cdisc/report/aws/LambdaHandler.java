package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.ReportContext;
import gov.nih.nci.evs.cdisc.report.TextExcelReportGenerator;
import gov.nih.nci.evs.cdisc.report.model.TextExcelReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LambdaHandler implements RequestHandler<LambdaRequest, LambdaResponse> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public LambdaResponse handleRequest(LambdaRequest input, Context context) {
    File file = new File("/mnt/cdisc");
    System.out.println("Executable: " + file.canExecute());
    System.out.println("Readable: " + file.canRead());
    System.out.println("Writable: " + file.canWrite());
    try {
      System.out.println("Owner: " + Files.getOwner(file.toPath()));
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<TextExcelReportResponse> reportResponseList = new ArrayList<>();
    for (String rootCode : input.getConceptCodes()) {
      ReportContext reportContext =
          new ReportContext(new File(input.getThesaurusOwlFile()), rootCode, getOutputDirectory());
      TextExcelReportResponse reportResponse =
          new TextExcelReportGenerator(reportContext).run(reportContext);
      log.info("TextExcelReportResponse:{}", reportResponse);
      reportResponseList.add(reportResponse);
    }
    return new LambdaResponse(reportResponseList, input.getPublicationDate());
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
