package gov.nih.nci.evs.cdisc.report.aws;

import lombok.Data;

import java.util.List;

@Data
public class LambdaRequest {
    private String thesaurusOwlFile;
    private List<String> rootConceptCodes;
    private String publicationDate;
}
