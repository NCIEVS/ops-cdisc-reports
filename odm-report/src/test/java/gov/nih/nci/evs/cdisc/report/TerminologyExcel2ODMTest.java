package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import gov.nih.nci.evs.test.utils.TestUtils;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import javax.xml.datatype.DatatypeConfigurationException;
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

public class TerminologyExcel2ODMTest {

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
    String outFile =
        Paths.get(outFolder.getAbsolutePath(), format("%s.odm.xml", subSource)).toString();
    // We cannot use the fixtures in the common project as the code just creates the output file in
    // the same directory as the XLS file. This would overwrite the existing ODM fixtures.
    TerminologyExcel2ODM terminologyExcel2ODM =
        new TerminologyExcel2ODM(
            TestUtils.getResourceFilePath("/fixtures/report-files/%s/%s.xls", subSource), outFile);
    terminologyExcel2ODM.generate_odm_xml();
    Diff myDiff =
        DiffBuilder.compare(
                IOUtils.resourceToString(
                    format(
                        "/fixtures/report-files/%s/%s.odm.xml",
                        ReportUtils.getShortCodeLabel(subSource), subSource),
                    Charset.defaultCharset()))
            .checkForSimilar()
            .withTest(IOUtils.toString(new FileInputStream(outFile), Charset.defaultCharset()))
            .withDifferenceEvaluator(new IgnoreAttributeDifferenceEvaluator(getIgnoreAttributes()))
            .build();
    printDifferences(myDiff);
    Assertions.assertFalse(myDiff.hasDifferences());
  }

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
  public void testGenerateOdmXmlV2(String subSource)
      throws IOException, InvalidFormatException, DatatypeConfigurationException, JAXBException {
    String outFile =
        Paths.get(outFolder.getAbsolutePath(), format("%s.odm.xml", subSource)).toString();
    // We cannot use the fixtures in the common project as the code just creates the output file in
    // the same directory as the XLS file. This would overwrite the existing ODM fixtures.
    OdmConvertorV2 odmConvertorV2 =
        new OdmConvertorV2(
            TestUtils.getResourceFilePath("/fixtures/report-files/%s/%s.xls", subSource), outFile);
    odmConvertorV2.generateOdmXml();
    Diff myDiff =
        DiffBuilder.compare(
                IOUtils.resourceToString(
                    format(
                        "/fixtures/report-files/%s/%s.odm.xml",
                        ReportUtils.getShortCodeLabel(subSource), subSource),
                    Charset.defaultCharset()))
            .checkForSimilar()
            .withTest(IOUtils.toString(new FileInputStream(outFile), Charset.defaultCharset()))
            .withDifferenceEvaluator(new IgnoreAttributeDifferenceEvaluator(getIgnoreAttributes()))
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
