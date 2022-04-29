package gov.nih.nci.evs.cdisc.report.model;

import gov.nih.nci.evs.cdisc.report.ReportResponse;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TextExcelReportResponse extends ReportResponse {
    private String label;
    private String textReportFileName;
    private String code;
}
