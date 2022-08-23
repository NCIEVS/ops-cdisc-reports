package gov.nih.nci.evs.cdisc.report.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class TextReportLineComparator implements Comparator<String> {
  @Override
  public int compare(String o1, String o2) {
    String[] line1Columns = o1.split("\t");
    String[] line2Columns = o2.split("\t");
    if (line1Columns[3] != null && line2Columns[3] == null) {
      return 1;
    } else if (line1Columns[3] == null && line2Columns[3] != null) {
      return -1;
    } else if ((line1Columns[3] == null && line2Columns[3] == null)
        || line1Columns[3].compareTo(line2Columns[3]) == 0) {
      String codeListCode1 = line1Columns[1];
      String codeListCode2 = line2Columns[1];
      if (isBlank(codeListCode1) && isNotBlank(codeListCode2)) {
        return -1;
      } else if (isNotBlank(codeListCode1) && isBlank(codeListCode2)) {
        return 1;
      } else if (StringUtils.isNoneBlank(codeListCode1, codeListCode2)) {
        return StringUtils.compare(getKey(line1Columns), getKey(line2Columns));
      }
    }

    return StringUtils.compare(line1Columns[3].toLowerCase(), line2Columns[3].toLowerCase());
  }

  private String getKey(String[] columns) {
    return String.join("$", columns[3], columns[4], columns[1]).toLowerCase();
  }
}
