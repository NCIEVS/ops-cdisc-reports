package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.test.utils.TestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class RDFGeneratorTest {

  @TempDir public File outFolder;

  @ParameterizedTest
  @CsvSource({
    "ADaM Terminology",
    "Glossary Terminology",
    "CDASH Terminology",
    "Define-XML Terminology",
    "Protocol Terminology",
    "SDTM Terminology",
    "SEND Terminology"
  })
  public void test_generate_odm_xml(String subSource) throws IOException {
    String outFile = Paths.get(outFolder.getAbsolutePath(), format("%s.owl", subSource)).toString();
    new RDFGenerator()
        .generate(
            TestUtils.getResourceFilePath("/fixtures/report-files/%s/%s.txt", subSource), outFile);
    Diff myDiff =
        DiffBuilder.compare(
                TestUtils.getResourceAsStream("/fixtures/report-files/%s/%s.owl", subSource))
            .checkForSimilar()
            .withTest(IOUtils.toString(new FileInputStream(outFile), Charset.defaultCharset()))
            .ignoreComments()
            .build();
    printDifferences(myDiff);
    Assertions.assertFalse(myDiff.hasDifferences());
  }

  private void printDifferences(Diff diff) {
    for (Difference difference : diff.getDifferences()) {
      System.out.println(difference.toString());
    }
  }

  private Map<String, List<String>> getIgnoreAttributes() {
    Map<String, List<String>> ignoreAttributeMap = new HashMap<>();
    List<String> ignoreAttributes = new ArrayList<>();
    ignoreAttributes.add("CreationDateTime");
    ignoreAttributeMap.put("ODM", ignoreAttributes);
    return ignoreAttributeMap;
  }
}
