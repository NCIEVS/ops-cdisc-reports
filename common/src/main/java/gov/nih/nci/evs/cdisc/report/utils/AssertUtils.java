package gov.nih.nci.evs.cdisc.report.utils;

public class AssertUtils {
  public static void assertRequired(Object value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(String.format("%s is required", fieldName));
    }
  }
}
