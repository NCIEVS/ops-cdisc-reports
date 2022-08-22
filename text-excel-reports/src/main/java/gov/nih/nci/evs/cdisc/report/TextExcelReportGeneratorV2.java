package gov.nih.nci.evs.cdisc.report;

import com.google.common.collect.Lists;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.report.util.SortComparatorV2;
import gov.nih.nci.evs.cdisc.report.util.TextReportLineComparator;
import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import gov.nih.nci.evs.cdisc.thesaurus.model.AlternativeDefinition;
import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import gov.nih.nci.evs.cdisc.thesaurus.owl.ThesaurusOwlReader;
import gov.nih.nci.evs.reportwriter.formatter.AsciiToExcelFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2022 Guidehouse. This software was developed in conjunction with the National Cancer
 * Institute, and so to the extent government employees are co-authors, any rights in such works
 * shall be subject to Title 17 of the United States Code, section 105. Redistribution and use in
 * source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met: 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the disclaimer of Article 3, below. Redistributions in binary form
 * must reproduce the above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution. 2. The end-user
 * documentation included with the redistribution, if any, must include the following
 * acknowledgment: "This product includes software developed by Guidehouse and the National Cancer
 * Institute." If no such end-user documentation is to be included, this acknowledgment shall appear
 * in the software itself, wherever such third-party acknowledgments normally appear. 3. The names
 * "The National Cancer Institute", "NCI" and "Guidehouse" must not be used to endorse or promote
 * products derived from this software. 4. This license does not authorize the incorporation of this
 * software into any third party proprietary programs. This license does not authorize the recipient
 * to use any trademarks owned by either NCI or GUIDEHOUSE 5. THIS SOFTWARE IS PROVIDED "AS IS," AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE DISCLAIMED. IN NO EVENT SHALL THE
 * NATIONAL CANCER INSTITUTE, GUIDEHOUSE, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 *
 * @author EVS Team
 * @version 1.0
 *     <p>Modification history: Initial implementation kim.ong@nih.gov
 */
@Slf4j
public class TextExcelReportGeneratorV2 {
  public static List<String> HEADERS =
      Lists.newArrayList(
          "Code",
          "Codelist Code",
          "Codelist Extensible (Yes/No)",
          "Codelist Name",
          "CDISC Submission Value",
          "CDISC Synonym(s)",
          "CDISC Definition",
          "NCI Preferred Term");
  public static final String tTERM_SOURCE_CDISC = "CDISC";
  public static final String TERM_SOURCE_CDISC_GLOSSARY = "CDISC-GLOSS";
  public static final String TERM_GROUP_SYNONYM = "SY";
  public static final String TERM_GROUP_PREFERRED_TERM = "PT";
  public static final String TEXT_FILE_DELIMITER = "\t";

  private final Path outDirectory;
  private final Map<String, Concept> conceptMap;

  public TextExcelReportGeneratorV2(File owlFile, Path outDirectory) {
    ThesaurusOwlReader owlReader = new ThesaurusOwlReader(owlFile);
    this.conceptMap = owlReader.createConceptMap();
    this.outDirectory = outDirectory;
  }

  public ReportDetail run(String root) {
    log.info("Subset root concept code: {}", root);
    Concept rootConcept = conceptMap.get(root);
    if (rootConcept == null) {
      throw new RuntimeException(String.format("Root concept %s not in owl file", root));
    }
    String label = rootConcept.getLabel();
    File textFile = getTextFile(label, outDirectory);
    log.info("Generating text file:{}", textFile.getName());

    boolean glossaryRootConcept = isGlossaryConcept(label);

    List<String> lines = new ArrayList<>();

    List<Concept> codeListCodes = new ArrayList<>();
    populateCodeListCodes(rootConcept, codeListCodes);
    if (codeListCodes.isEmpty()) {
      codeListCodes.add(rootConcept);
    }

    populateCodeInSubsets(codeListCodes);

    for (int i = 0; i < codeListCodes.size(); i++) {
      log.info("Processing Code list {} of {}", i, codeListCodes.size());
      Concept concept = codeListCodes.get(i);
      log.info("Processing {}", concept.getCode());
      String submissionValue =
          getSubmissionValue(concept, getExpectedTermSource(glossaryRootConcept));
      if (StringUtils.isNotBlank(submissionValue) && !"null".equalsIgnoreCase(submissionValue)) {
        lines.add(getCodeListConceptLine(concept, submissionValue, glossaryRootConcept));
        List<Concept> members = concept.getCodeInSubsets();
        for (int j = 0; j < members.size(); j++) {
          log.info(
              "Processing member {} of {}. Concept index:{}. Concept code:{}",
              j,
              members.size(),
              i,
              concept.getCode());
          Concept member = members.get(j);
          if (!member.isRetired()) {
            lines.add(getMemberConceptLine(member, concept, glossaryRootConcept));
          }
        }
      }
    }
    lines.sort(new TextReportLineComparator());
    lines.add(0, String.join(TEXT_FILE_DELIMITER, HEADERS));
    saveToFile(textFile, lines);
    String excelFileName = generateExcel(textFile);

    Map<ReportEnum, String> reports = new HashMap<>();
    reports.put(ReportEnum.MAIN_TEXT, textFile.getAbsolutePath());
    reports.put(ReportEnum.MAIN_EXCEL, excelFileName);
    return ReportDetail.builder().code(root).label(label).reports(reports).build();
  }

  private boolean isGlossaryConcept(String label) {
    return label.contains("Glossary");
  }

  private String getSubmissionValue(Concept concept, String expectedTermSource) {
    return concept.getSynonyms().stream()
        .filter(
            synonym ->
                expectedTermSource.equals(synonym.getTermSource())
                    && TERM_GROUP_PREFERRED_TERM.equals(synonym.getTermGroup()))
        .map(Synonym::getTermName)
        .findFirst()
        .orElse("");
  }

  private String getSynonym(Concept concept, String expectedTermSource) {
    return concept.getSynonyms().stream()
        .filter(
            synonym ->
                expectedTermSource.equals(synonym.getTermSource())
                    && TERM_GROUP_SYNONYM.equals(synonym.getTermGroup()))
        .map(Synonym::getTermName)
        .sorted(new SortComparatorV2())
        .collect(Collectors.joining("; "));
  }

  private String getCdiscDefinition(Concept concept, boolean glossaryRootConcept) {
    if (concept.getAlternativeDefinitions() == null) {
      return "";
    }
    return concept.getAlternativeDefinitions().stream()
        .filter(alternativeDefinition -> !alternativeDefinition.isCdisc() == glossaryRootConcept)
        .map(AlternativeDefinition::getDefinition)
        .reduce(
            (first, second) ->
                second) // Get the last value. This is match what the current report generator is
        // doing
        .orElse("");
  }

  private long getSourcePTCount(Concept concept, String termSource) {
    return concept.getSynonyms().stream()
        .filter(
            synonym ->
                termSource.equals(synonym.getTermSource())
                    && TERM_GROUP_PREFERRED_TERM.equals(synonym.getTermGroup()))
        .count();
  }

  private String getSubmissionValue(
      Concept memberConcept, Concept currentConcept, String termSource) {
    long knt = getSourcePTCount(memberConcept, termSource);
    if (knt == 1L) {
      log.trace("One PT synonym found for {}", memberConcept.getCode());
      return getSubmissionValue(memberConcept, termSource);
    }

    // find NCI AB of codeListCode:
    final String termName =
        currentConcept.getSynonyms().stream()
            .filter(
                synonym ->
                    "NCI".equals(synonym.getTermSource()) && "AB".equals(synonym.getTermGroup()))
            .map(Synonym::getTermName)
            .findFirst()
            .orElse(null);
    if (termName == null) {
      log.debug(
          "No NCI AB found, code:{} codeListCode:{}",
          memberConcept.getCode(),
          currentConcept.getCode());
      return getSubmissionValue(memberConcept, termSource);
    }

    return memberConcept.getSynonyms().stream()
        .filter(
            synonym ->
                termName.equals(synonym.getSourceCode())
                    && termSource.equals(synonym.getTermSource())
                    && TERM_GROUP_PREFERRED_TERM.equals(synonym.getTermGroup()))
        .map(Synonym::getTermName)
        .findFirst()
        .orElse(termName);
  }

  private String decodeSpecialChar(String line) {
    line = line.replaceAll("&apos;", "'");
    line = line.replaceAll("&amp;", "&");
    line = line.replaceAll("&lt;", "<");
    line = line.replaceAll("&gt;", ">");
    line = line.replaceAll("&quot;", "\"");
    return line;
  }

  private String getCodeListConceptLine(
      Concept concept, String submissionValue, boolean glossaryRootConcept) {
    List<String> line = new ArrayList<>();
    String termGroupSynonym = getSynonym(concept, getExpectedTermSource(glossaryRootConcept));
    line.add(concept.getCode());
    line.add("");
    Boolean extensible = concept.getExtensible();
    String strExtensible = extensible != null ? (extensible ? "Yes" : "No") : "";
    line.add(strExtensible);
    line.add(termGroupSynonym);
    line.add(submissionValue);
    line.add(termGroupSynonym);
    line.add(getCdiscDefinition(concept, glossaryRootConcept));
    line.add(concept.getPreferredName());
    return decodeSpecialChar(String.join(TEXT_FILE_DELIMITER, line));
  }

  private String getMemberConceptLine(
      Concept member, Concept concept, boolean glossaryRootConcept) {
    String submissionValue =
        getSubmissionValue(member, concept, getExpectedTermSource(glossaryRootConcept));
    String synonymTermName = getSynonym(member, getExpectedTermSource(glossaryRootConcept));
    String cdiscDefinition = getCdiscDefinition(member, glossaryRootConcept);
    String preferredName = member.getPreferredName();
    return decodeSpecialChar(
        String.join(
            TEXT_FILE_DELIMITER,
            member.getCode(),
            concept.getCode(),
            "",
            getSynonym(concept, getExpectedTermSource(glossaryRootConcept)),
            submissionValue,
            synonymTermName,
            cdiscDefinition,
            preferredName));
  }

  private void saveToFile(File outputFile, List<String> v) {
    try (FileOutputStream output = new FileOutputStream(outputFile)) {
      IOUtils.writeLines(v, System.lineSeparator(), output, Charset.defaultCharset());
    } catch (IOException e) {
      throw new RuntimeException("Error occurred when creating text report", e);
    }
  }

  private void populateCodeListCodes(Concept rootConcept, List<Concept> allDescendantCodes) {
    List<Concept> childListCodes =
        this.conceptMap.values().stream()
            .filter(concept -> concept.getParents().contains(rootConcept.getCode()))
            .collect(Collectors.toList());
    allDescendantCodes.addAll(childListCodes);
    for (Concept concept : childListCodes) {
      populateCodeListCodes(concept, allDescendantCodes);
    }
  }

  private void populateCodeInSubsets(List<Concept> codeListCodes) {
    for (Concept concept : codeListCodes) {
      concept
          .getCodeInSubsets()
          .addAll(
              this.conceptMap.values().stream()
                  .filter(
                      subsetConcept -> subsetConcept.getSubsetCodes().contains(concept.getCode()))
                  .collect(Collectors.toList()));
    }
  }

  private String getExpectedTermSource(boolean glossary) {
    return glossary ? TERM_SOURCE_CDISC_GLOSSARY : tTERM_SOURCE_CDISC;
  }

  private File getTextFile(String label, Path outputDirectory) {
    String shortLabel = ReportUtils.getShortCodeLabel(label);
    String textFileName = label.replace("CDISC", "").trim() + ".txt";
    return ReportUtils.getOutputPath(outputDirectory, shortLabel).resolve(textFileName).toFile();
  }

  private String generateExcel(File textfile) {
    String excelFileName = textfile.getAbsolutePath().replace(".txt", ".xls");
    try {
      new AsciiToExcelFormatter()
          .convert(textfile.getAbsolutePath(), TEXT_FILE_DELIMITER, excelFileName);
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Error occurred while creating excel file. File name:%s", excelFileName),
          e);
    }
    return excelFileName;
  }

  public static void main(String[] args) {
    if (args == null || args.length != 3) {
      System.out.println(
          "Command line parameters: (1) ThesaurusInferred_forTS.owl  (2): Root concept code (e.g., C77526) (3): Output directory");
      System.exit(1);
    }
    File owlfile = new File(args[0]);
    String root = args[1];
    Path outputDirectory = Paths.get(args[2]);
    new TextExcelReportGeneratorV2(owlfile, outputDirectory).run(root);
  }
}
