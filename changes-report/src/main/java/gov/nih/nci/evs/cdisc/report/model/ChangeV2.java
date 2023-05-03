package gov.nih.nci.evs.cdisc.report.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangeV2 {
  private String releaseDate;
  private String requestCode;
  private String changeType;
  private String code;
  private String termType;
  private String codeListShortName;
  private String codeListLongName;
  private String changeSummary;
  private String originalValue;
  private String newValue;

  public int getNoCCode() {
    return Integer.parseInt(code.replace("C", ""));
  }

  public String toLine() {
    return String.join(
            "\t",
            this.releaseDate,
            this.requestCode,
            this.changeType,
            this.code,
            this.termType,
            this.codeListShortName,
            this.codeListLongName,
            this.changeSummary,
            this.originalValue,
            this.newValue);
  }
}
