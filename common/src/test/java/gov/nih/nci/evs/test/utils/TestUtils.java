package gov.nih.nci.evs.test.utils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getShortCodeLabel;

public class TestUtils {
  public static String getResourceFilePath(String resource) {
    String resourceUrl = TestUtils.class.getResource(resource).getPath();
    try {
      return java.net.URLDecoder.decode(resourceUrl, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return resourceUrl;
    }
  }

  public static String getResourceFilePath(String resource, String subSource) {
    return getResourceFilePath(String.format(resource, getShortCodeLabel(subSource), subSource));
  }

  public static InputStream getResourceAsStream(String resource, String subSource) {
    return TestUtils.class.getResourceAsStream(
        String.format(resource, getShortCodeLabel(subSource), subSource));
  }
}
