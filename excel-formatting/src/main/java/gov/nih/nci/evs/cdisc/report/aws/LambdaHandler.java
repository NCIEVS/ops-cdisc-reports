package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.CDISCExcelUtils;
import gov.nih.nci.evs.cdisc.report.ReportEnum;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    validate(input);
    for (ReportDetail reportDetail : input.getReportDetails()) {
      String xlsFile = reportDetail.getReports().get(ReportEnum.MAIN_EXCEL);
      String newFile = (new CDISCExcelUtils(xlsFile, input.getPublicationDate())).reformat(xlsFile);
      String title = FilenameUtils.getBaseName(xlsFile);
      String author = "NCI-EVS";
      CDISCExcelUtils cdiscExcelUtils = new CDISCExcelUtils(newFile);
      cdiscExcelUtils.setMetadata(title, author, title, title, null);
      cdiscExcelUtils.saveWorkbook(xlsFile);
    }
    // The Excel file report gets formatted in place, so
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(request.getPublicationDate(), "publicationDate");
  }
}
