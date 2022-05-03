package gov.nih.nci.evs.cdisc.report.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

public class AssertUtils {
  public static void assertRequired(Object value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(String.format("%s is required", fieldName));
    }
  }

  public static void assertRequired(String value, String fieldName) {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(String.format("%s is required", fieldName));
    }
  }

  public static void assertRequired(Collection value, String fieldName) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(String.format("%s is required", fieldName));
    }
  }
}
