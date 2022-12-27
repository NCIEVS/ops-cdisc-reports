package gov.nih.nci.evs.cdisc.report.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PreferredTermPair {
  private String matchingConceptCode;
  private String matchingCdiscSynonym;
  private String cdiscPreferredTerm;
  private String sourceCode;
  private String conceptCode;
}
