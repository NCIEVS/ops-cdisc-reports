package gov.nih.nci.evs.cdisc.report.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReportSummary {
    private List<ReportDetail> reportDetails;
    private String publicationDate;
    private List<String> deliveryEmailAddresses;
    private Integer deleteOldReportsThresholdDays;
}
