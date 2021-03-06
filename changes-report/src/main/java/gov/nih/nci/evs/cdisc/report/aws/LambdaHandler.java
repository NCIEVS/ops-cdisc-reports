package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.ChangesReport;
import gov.nih.nci.evs.cdisc.report.ReportEnum;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getBaseOutputDirectory;
import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getShortCodeLabel;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  /**
   * Creates changes report. It is a report that compares the most recent prior text reports created
   * by text-excel-report with the current text report. Skip creating report if there were no prior
   * year or current year report
   *
   * @param input reports that have already been created. This would give path to the current text
   *     report
   * @param context lambda context
   * @return augmented report list with the changes report if one was created
   */
  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    validate(input);
    for (ReportDetail reportDetail : input.getReportDetails()) {
      ChangesReport report = new ChangesReport();
      String shortCodeLabel = getShortCodeLabel(reportDetail.getLabel());
      String absoluteReportFileName = reportDetail.getReports().get(ReportEnum.MAIN_TEXT);
      String previousReportFileName = getPreviousReportFileName(absoluteReportFileName);
      String changesReportFileName = getReportFileName(shortCodeLabel, getBaseOutputDirectory());

      if (!new File(previousReportFileName).exists()) {
        log.info("No previous report found for {}. Skipping changes report.", shortCodeLabel);
        continue;
      }
      log.info("Initializing diff report. code:{}", shortCodeLabel);
      report.init(
          absoluteReportFileName,
          previousReportFileName,
          input.getPublicationDate(),
          changesReportFileName);
      log.info("Getting changes. code:{}", shortCodeLabel);
      report.getChanges();
      log.info("Printing changes report. code:{}", shortCodeLabel);
      report.print();
      reportDetail.getReports().put(ReportEnum.CHANGES_TEXT, changesReportFileName);
    }
    // The Excel file report gets formatted in place, so
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(request.getPublicationDate(), "publicationDate");
  }

  private String getPreviousReportFileName(String absoluteReportFileName) {
    String reportFileName = FilenameUtils.getName(absoluteReportFileName);
    Path previousReportsDirectory = Path.of("/mnt", "cdisc", "work", "previous");
    if (!previousReportsDirectory.toFile().exists()) {
      throw new RuntimeException(
          String.format(
              "Directory with previous reports does not exist. Previous directory:%s",
              previousReportsDirectory));
    }
    return previousReportsDirectory.resolve(reportFileName).toString();
  }

  private String getReportFileName(String codeLabel, Path outputDirectory) {
    String changeReportFileName = new StringBuilder(codeLabel).append(" Changes.txt").toString();
    return ReportUtils.getOutputPath(outputDirectory, codeLabel)
        .resolve(changeReportFileName)
        .toString();
  }
}
