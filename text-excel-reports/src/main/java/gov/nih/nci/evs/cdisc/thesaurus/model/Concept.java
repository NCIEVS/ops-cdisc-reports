package gov.nih.nci.evs.cdisc.thesaurus.model;

import gov.nih.nci.evs.cdisc.report.model.Synonym;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class Concept {
  private String code;
  private String label;
  private String preferredName;
  private boolean retired;
  private Boolean extensible;

  @Getter(lazy = true)
  private final List<String> parents = new ArrayList<>();

  @Getter(lazy = true)
  private final List<String> subsetCodes = new ArrayList<>();

  @Getter(lazy = true)
  private final List<Concept>  codeInSubsets = new ArrayList<>();

  @Getter(lazy = true)
  private final List<Synonym> synonyms = new ArrayList<>();

  @Getter(lazy = true)
  private final List<AlternativeDefinition> alternativeDefinitions = new ArrayList<>();
}
