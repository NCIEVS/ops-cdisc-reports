package gov.nih.nci.evs.cdisc.report.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TargetTermPair {
    private String conceptCode;
    private String cdiscPreferredTerm;
    private String cdiscSynonym;
}
