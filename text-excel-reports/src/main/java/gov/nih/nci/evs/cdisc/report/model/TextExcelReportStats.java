package gov.nih.nci.evs.cdisc.report.model;

import gov.nih.nci.evs.cdisc.report.ReportStats;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TextExcelReportStats extends ReportStats {
    private int numberOfLines;
}
