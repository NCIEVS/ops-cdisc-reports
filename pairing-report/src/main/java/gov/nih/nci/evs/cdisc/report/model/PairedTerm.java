package gov.nih.nci.evs.cdisc.report.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PairedTerm {
    private String memberCode;
    private String code;
    private String cdiscSynonym;
    private String cdiscPreferredTerm;
    private String submissionValueCode;

    public String toLine(){
        return String.join("|", memberCode, cdiscSynonym, cdiscPreferredTerm);
    }
}
