package gov.nih.nci.evs.cdisc.report.model;

import lombok.Data;

import java.util.List;

@Data
public class ThesaurusRequest {
    private String thesaurusOwlFile;
    private String publicationDate;
    private List<String> deliveryEmailAddresses;
    private List<String> conceptCodes;
}
