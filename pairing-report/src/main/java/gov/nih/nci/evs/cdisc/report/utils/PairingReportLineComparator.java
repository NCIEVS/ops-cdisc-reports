package gov.nih.nci.evs.cdisc.report.utils;

import gov.nih.nci.evs.cdisc.report.CDISCPairingV2;

import java.util.Comparator;

/** Comparator to sort lines in the pairing report. Assumes that there are no null lines */
public class PairingReportLineComparator implements Comparator<String> {
  @Override
  public int compare(String o1, String o2) {
    String[] cells1 = o1.split(CDISCPairingV2.TEXT_FILE_DELIMITER);
    String[] cells2 = o2.split(CDISCPairingV2.TEXT_FILE_DELIMITER);
    int compareCodeListCode = cells1[1].compareTo(cells2[1]);
    if (compareCodeListCode == 0) {
      // within the code list, compare by member code
      return cells1[0].compareTo(cells2[0]);
    }
    return compareCodeListCode;
  }
}
