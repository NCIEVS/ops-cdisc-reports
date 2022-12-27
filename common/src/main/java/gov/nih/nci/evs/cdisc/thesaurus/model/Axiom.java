package gov.nih.nci.evs.cdisc.thesaurus.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Axiom {
    private String annotatedSource;
    private String annotatedProperty;
    private String annotatedTarget;
    private Map<String, String> elements = new HashMap<>();
}
