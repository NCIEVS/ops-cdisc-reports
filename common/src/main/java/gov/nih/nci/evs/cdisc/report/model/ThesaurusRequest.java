package gov.nih.nci.evs.cdisc.report.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ThesaurusRequest {
    private String thesaurusOwlFile;
    private String publicationDate;
    private List<String> deliveryEmailAddresses;
    private List<String> conceptCodes;
}
