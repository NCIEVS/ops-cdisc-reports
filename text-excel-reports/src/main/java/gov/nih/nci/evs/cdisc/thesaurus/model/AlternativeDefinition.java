package gov.nih.nci.evs.cdisc.thesaurus.model;

import lombok.Data;

@Data
public class AlternativeDefinition {
    private String conceptCode;
    private String definition;
    private boolean cdisc;
}
