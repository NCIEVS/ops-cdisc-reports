package gov.nih.nci.evs.cdisc.report;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReportResponse {
    private ReportStats stats;
}
