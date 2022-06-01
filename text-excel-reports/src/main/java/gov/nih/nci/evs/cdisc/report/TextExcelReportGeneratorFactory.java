package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;

import java.io.File;

public class TextExcelReportGeneratorFactory {
    public TextExcelReportGenerator createTextExcelReportGenerator(File thesaurusOwlFile){
        return new TextExcelReportGenerator(thesaurusOwlFile, ReportUtils.getBaseOutputDirectory());
    }
}
