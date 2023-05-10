package gov.nih.nci.evs.cdisc.report.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Terminology {
  private String model;
  private String shortModel;
  private String type;
  private String date;
  private int sheetIndex;
}
