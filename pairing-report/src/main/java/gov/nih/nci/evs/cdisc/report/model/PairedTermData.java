package gov.nih.nci.evs.cdisc.report.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import static gov.nih.nci.evs.cdisc.thesaurus.utils.ThesaurusUtils.decodeSpecialChar;

@Data
@AllArgsConstructor
public class PairedTermData {
  private String memberCode;
  private String sourceCode;
  private String name;
  private String cdiscPreferredTerm;
  private PairedTerm targetTerm;
  private String synonyms;
  private String cdiscDefinition;
  private String preferredTerm;

  public String toLine() {
    return String.join(
        "|",
        memberCode,
        sourceCode,
        name,
        cdiscPreferredTerm,
        decodeSpecialChar(targetTerm.toLine()),
        synonyms,
        cdiscDefinition,
        preferredTerm);
  }
}
