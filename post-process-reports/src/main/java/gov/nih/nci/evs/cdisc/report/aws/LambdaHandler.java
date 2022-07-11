package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.PostProcessService;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LambdaHandler implements RequestHandler<List, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  /**
   * Consolidates and flattens all the responses created by the various lambdas. Also copies the
   * reports to specific folders according to requirements
   *
   * @param input contains paths to all the reports created
   * @param context lambda context
   * @return flattened reports list
   */
  @Override
  public ReportSummary handleRequest(List input, Context context) {
    PostProcessService postProcessService = new PostProcessService();
    ReportSummary reportSummary = postProcessService.getReportSummary(input);
    postProcessService.archiveFiles(reportSummary);
    return reportSummary;
  }
}
