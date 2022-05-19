package gov.nih.nci.evs.cdisc.report.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.function.Predicate;

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

  public static <T> void assertCondition(Predicate<T> value, T object, String fieldName) {
    if (value.test(object)) {
      throw new IllegalArgumentException(String.format("%s failed precondition", fieldName));
    }
  }
}
