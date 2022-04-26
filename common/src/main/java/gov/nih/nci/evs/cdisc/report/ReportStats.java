package gov.nih.nci.evs.cdisc.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportStats {
    private LocalDateTime start;
    private LocalDateTime end;
}
