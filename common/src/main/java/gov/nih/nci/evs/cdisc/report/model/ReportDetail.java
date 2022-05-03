package gov.nih.nci.evs.cdisc.report.model;

import gov.nih.nci.evs.cdisc.report.ReportEnum;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ReportDetail {
    private String code;
    private String label;
    private Map<ReportEnum, String> reports;
}
