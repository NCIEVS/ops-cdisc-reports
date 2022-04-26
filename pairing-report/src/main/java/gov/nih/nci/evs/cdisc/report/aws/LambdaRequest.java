package gov.nih.nci.evs.cdisc.report.aws;

import lombok.Data;

@Data
public class LambdaRequest {
    private String thesaurusOwlFile;
    private String rootConceptCode;
}
