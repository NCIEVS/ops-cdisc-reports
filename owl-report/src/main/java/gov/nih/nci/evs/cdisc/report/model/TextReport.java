package gov.nih.nci.evs.cdisc.report.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextReport {
  private String code;
  private String codeListCode;
  private String codeListExtensible;
  private String codeListName;
  private String cdiscSubmissionValue;
  private String cdiscSynonyms;
  private String cdiscDefinition;
  private String nciPreferredTerm;
}
