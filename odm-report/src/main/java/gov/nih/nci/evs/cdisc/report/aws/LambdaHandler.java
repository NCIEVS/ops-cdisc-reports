package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import gov.nih.nci.evs.cdisc.report.ReportEnum;
import gov.nih.nci.evs.cdisc.report.TerminologyExcel2ODM;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LambdaHandler implements RequestHandler<ReportSummary, ReportSummary> {
  Logger log = LoggerFactory.getLogger(LambdaHandler.class);

  @Override
  public ReportSummary handleRequest(ReportSummary input, Context context) {
    validate(input);
    for (ReportDetail reportDetail : input.getReportDetails()) {
      String excelFile = reportDetail.getReports().get(ReportEnum.MAIN_EXCEL);
      if (excelFile != null && new File(excelFile).exists()) {
        String baseName = FilenameUtils.removeExtension(excelFile);
        String odmXmlFile = baseName + ".odm.xml";
        log.info("Generating {} from {}", odmXmlFile, excelFile);
        TerminologyExcel2ODM generator = new TerminologyExcel2ODM(excelFile, odmXmlFile);
        generator.generate_odm_xml();
        reportDetail.getReports().put(ReportEnum.ODM_XML, odmXmlFile);
      } else {
        throw new RuntimeException(String.format("File %s does not exist.", excelFile));
      }
    }
    // The Excel file report gets formatted in place, so
    return input;
  }

  private void validate(ReportSummary request) {
    AssertUtils.assertRequired(request.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(request.getPublicationDate(), "publicationDate");
  }
}
