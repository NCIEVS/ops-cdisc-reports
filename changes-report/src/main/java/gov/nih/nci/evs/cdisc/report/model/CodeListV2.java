package gov.nih.nci.evs.cdisc.report.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class CodeListV2 {
  private String code;
  private String extensible;
  private String name;
  private String subValue;
  private String[] synonyms;
  private String definition;
  private String preferredTerm;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CodeListV2 codeList = (CodeListV2) o;
    return code.equals(codeList.getCode());
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }
}
