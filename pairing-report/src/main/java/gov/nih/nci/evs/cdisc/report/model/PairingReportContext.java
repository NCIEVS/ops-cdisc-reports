package gov.nih.nci.evs.cdisc.report.model;

import gov.nih.nci.evs.cdisc.report.ReportContext;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class PairingReportContext extends ReportContext {
    private int dataSource;
}
