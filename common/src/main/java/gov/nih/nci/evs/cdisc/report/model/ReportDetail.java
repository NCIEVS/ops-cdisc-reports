package gov.nih.nci.evs.cdisc.report.model;

import gov.nih.nci.evs.cdisc.report.ReportEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportDetail {
    private String code;
    private String label;
    private Map<ReportEnum, String> reports;
}
