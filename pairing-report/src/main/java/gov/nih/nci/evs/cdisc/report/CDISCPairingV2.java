package gov.nih.nci.evs.cdisc.report;

import com.google.common.collect.ImmutableList;
import gov.nih.nci.evs.cdisc.report.model.PreferredTermPair;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.report.model.TargetTermPair;
import gov.nih.nci.evs.cdisc.report.utils.PairingReportLineComparator;
import gov.nih.nci.evs.cdisc.report.utils.XLSXFormatter;
import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import gov.nih.nci.evs.cdisc.thesaurus.owl.ThesaurusOwlReader;
import gov.nih.nci.evs.cdisc.thesaurus.util.SortComparatorV2;
import gov.nih.nci.evs.cdisc.thesaurus.util.ThesaurusUtil;
import gov.nih.nci.evs.restapi.util.ExcelReadWriteUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;
import static gov.nih.nci.evs.cdisc.thesaurus.util.ThesaurusUtil.*;

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
public class CDISCPairingV2 {
  public static final String TEXT_FILE_DELIMITER = "|";
  public static List<String> METADATA_HEADER =
      ImmutableList.of(
          "Code",
          "Codelist Code",
          "Codelist Extensible (Yes/No)",
          "Codelist Name",
          "CDISC Submission Value",
          "CDISC Synonym(s)",
          "CDISC Definition",
          "NCI Preferred Term");
  public static List<String> PAIRED_TERMS_HEADER =
      ImmutableList.of(
          "Code",
          "TESTCD/PARMCD Codelist Code",
          "TESTCD/PARMCD Codelist Name",
          "TESTCD/PARMCD CDISC Submission Value Code",
          "TEST/PARM Codelist Code",
          "TEST/PARM Codelist Name",
          "TEST/PARM CDISC Submission Value Name",
          "CDISC Synonym(s)",
          "CDISC Definition",
          "NCI Preferred Term");

  private final Path outputDirectory;
  private final String publicationDate;
  private final Map<String, Concept> conceptMap;

  private final List<Concept> codeListCodes = new ArrayList<>();

  public CDISCPairingV2(File owlFile, Path outputDirectory, String publicationDate) {
    assertRequired(owlFile, "owlFile");
    assertRequired(outputDirectory, "outputDirectory");
    assertRequired(publicationDate, "publicationDate");

    try {
      LocalDate.parse(publicationDate);
    } catch (DateTimeParseException dtpe) {
      throw new RuntimeException("Invalid publication date. Expecting YYYY-mm-DD", dtpe);
    }

    this.conceptMap = new ThesaurusOwlReader(owlFile).createConceptMap();
    this.outputDirectory = outputDirectory;
    this.publicationDate = publicationDate;
  }

  public ReportDetail run(String root) {
    Concept rootConcept = conceptMap.get(root);
    populateCodeListCodes(rootConcept, conceptMap.values(), codeListCodes);
    populateCodeInSubsets(conceptMap.values(), codeListCodes);
    String label = rootConcept.getLabel();
    boolean glossaryRootConcept = isGlossaryConcept(label);
    List<PreferredTermPair> pairedSourceTermData = generatePairedSourceTermData();
    List<String> lines = generatePairedTermData(pairedSourceTermData, glossaryRootConcept);
    lines.sort(new PairingReportLineComparator());
    String paringReportFile = writeTextReport(lines, "paired_terms.txt");
    String metadataReportFile = writeMetadataReport(lines, glossaryRootConcept);
    String excelFileName =
        writeExcelReport(
            root,
            ImmutableList.of(
                writeTextReport(Collections.emptyList(), "readme.txt"),
                paringReportFile,
                metadataReportFile));
    return ReportDetail.builder()
        .code(root)
        .label(label)
        .reports(Collections.singletonMap(ReportEnum.PAIRING_EXCEL, excelFileName))
        .build();
  }

  private String writeExcelReport(String terminology, List<String> textFiles) {
    String excelFileName =
        getAbsoluteOutputFilePath(terminology + "_paired_view_" + publicationDate + ".xlsx");

    List<String> sheetNames =
        ImmutableList.of(
            "ReadMe",
            terminology + " " + "Paired Codelist Metadata",
            terminology + " " + "Paired Terms");
    try {
      System.out.println("Generating Excel report. Please wait ...");
      ExcelReadWriteUtils.writeXLSXFile(
          excelFileName, new Vector<>(textFiles), new Vector<>(sheetNames), '|');
    } catch (Exception ex) {
      throw new RuntimeException("Exception occurred when writing Excel file", ex);
    }
    XLSXFormatter.reformat(excelFileName, excelFileName);
    return excelFileName;
  }

  private String getAbsoluteOutputFilePath(String fileName) {
    return outputDirectory.resolve(fileName).toString();
  }

  private String writeTextReport(List<String> lines, String reportFileName) {
    String absoluteReportFileName = getAbsoluteOutputFilePath(reportFileName);
    try {
      IOUtils.writeLines(lines, System.lineSeparator(), new FileWriter(absoluteReportFileName));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return absoluteReportFileName;
  }

  public String writeMetadataReport(List<String> pairedTermData, boolean glossaryRootConcept) {
    List<String> metadataReportLines = new ArrayList<>();
    metadataReportLines.add(String.join(TEXT_FILE_DELIMITER, METADATA_HEADER));
    for (String line : pairedTermData) {
      String[] cells = line.split("\\" + TEXT_FILE_DELIMITER);
      String code = cells[0];
      Concept concept = conceptMap.get(code);
      String codeListCode = cells[1];
      String codeListName = cells[2];
      String submissionValue = cells[3];
      String sourceMetadataLine =
          getMetadataLine(
              concept, codeListCode, codeListName, submissionValue, glossaryRootConcept);
      metadataReportLines.add(decodeSpecialChar(sourceMetadataLine));

      codeListCode = cells[4];
      codeListName = cells[5];
      submissionValue = cells[6];
      String targetMetadataLine =
          getMetadataLine(
              concept, codeListCode, codeListName, submissionValue, glossaryRootConcept);
      metadataReportLines.add(decodeSpecialChar(sourceMetadataLine));
      metadataReportLines.add(decodeSpecialChar(targetMetadataLine));
    }
    return writeTextReport(metadataReportLines, "paired_code_list_metadata.txt");
  }

  private String getMetadataLine(
      Concept concept,
      String codeListCode,
      String codeListName,
      String submissionValue,
      boolean glossaryRootConcept) {
    String strExtensible = getExtensibleString(codeListCode, conceptMap);
    String synonymsAsString = getSynonymsAsString(concept.getSynonyms());
    String definition = getCdiscDefinition(concept, glossaryRootConcept);
    String preferredName = concept.getPreferredName();
    String metaDataLine =
        String.join(
            TEXT_FILE_DELIMITER,
            concept.getCode(),
            codeListCode,
            strExtensible,
            codeListName,
            submissionValue,
            synonymsAsString,
            definition,
            preferredName);
    return decodeSpecialChar(metaDataLine);
  }

  private List<PreferredTermPair> generatePairedSourceTermData() {
    List<PreferredTermPair> pairs = new ArrayList<>();
    for (Concept concept : codeListCodes) {
      List<Synonym> synonymsWithSourceCode = getSynonymsWithSourceCode(concept.getSynonyms());
      if (synonymsWithSourceCode != null) {
        List<Synonym> submissionValueCodes = getSubmissionValueCodes(synonymsWithSourceCode);
        for (Synonym synonymWithSourceCode : submissionValueCodes) {
          Concept matchingConcept = getMatchingConcept(synonymWithSourceCode.getSourceCode());
          if (matchingConcept != null) {
            Synonym matchingSySynonym = ThesaurusUtil.getSySynonym(matchingConcept.getSynonyms());
            PreferredTermPair pair =
                PreferredTermPair.builder()
                    .matchingConceptCode(matchingConcept.getCode())
                    .matchingCdiscSynonym(
                        matchingSySynonym != null ? matchingSySynonym.getTermName() : null)
                    .sourceCode(concept.getCode())
                    .build();
            pairs.add(pair);
          }
        }
      }
    }
    return pairs;
  }

  private Concept getMatchingConcept(String sourceCode) {
    return codeListCodes.stream()
        .filter(
            concept ->
                concept.getSynonyms() != null
                    && concept.getSynonyms().stream()
                        .anyMatch(
                            synonym ->
                                TERM_GROUP_AB.equals(synonym.getTermGroup())
                                    && TERM_SOURCE_NCI.equals(synonym.getTermSource())
                                    && sourceCode.equals(synonym.getTermName())))
        .findFirst()
        .orElse(null);
  }

  private List<Synonym> getSubmissionValueCodes(List<Synonym> synonymsWithSourceCode) {
    return synonymsWithSourceCode.stream()
        .filter(
            synonym ->
                synonymsWithSourceCode.stream()
                    .map(Synonym::getSourceCode)
                    .collect(Collectors.toList())
                    .contains(synonym.getSourceCode().replace("CD", "")))
        .collect(Collectors.toList());
  }

  private List<String> generatePairedTermData(
      List<PreferredTermPair> pairedSourceTermData, boolean glossaryRootConcept) {
    List<String> pairedTermData = new ArrayList<>();
    pairedTermData.add(String.join(TEXT_FILE_DELIMITER, PAIRED_TERMS_HEADER));
    for (PreferredTermPair preferredTermPair : pairedSourceTermData) {
      Concept currentConcept = this.conceptMap.get(preferredTermPair.getConceptCode());
      String name = preferredTermPair.getMatchingCdiscSynonym(); // Laboratory Test Code
      String cdiscPreferredTerm =
          preferredTermPair.getCdiscPreferredTerm(); // cdisc pt matching submission code: CD code

      String value = preferredTermPair.getSourceCode(); // CD code
      String replacedName =
          StringUtils.replaceEach(
              name,
              new String[] {"Parameter Code", "Test Code", "Short Name", "Parameters Code"},
              new String[] {"Parameter Long Name", "Test Name", "Long Name", "Parameters"});
      String submissionValueCode = value;
      if (submissionValueCode.contains("CD")) {
        submissionValueCode = submissionValueCode.replace("CD", "");
      }
      String synonyms = getSynonymsAsString(currentConcept.getSynonyms());
      String definition = getCdiscDefinition(currentConcept, glossaryRootConcept);
      String preferredName = currentConcept.getPreferredName();
      TargetTermPair targetTermPair =
          searchPairedTargetTerm(currentConcept.getCode(), replacedName, submissionValueCode);
      String targetData =
          decodeSpecialChar(
              String.join(
                  TEXT_FILE_DELIMITER,
                  targetTermPair.getConceptCode(),
                  targetTermPair.getCdiscSynonym(),
                  targetTermPair.getCdiscPreferredTerm()));
      String line =
          String.join(
              TEXT_FILE_DELIMITER,
              currentConcept.getCode(),
              preferredTermPair.getMatchingConceptCode(),
              preferredTermPair.getCdiscPreferredTerm(),
              cdiscPreferredTerm,
              decodeSpecialChar(targetData),
              synonyms,
              definition,
              preferredName);
      if (!currentConcept.isRetired()) {
        pairedTermData.add(line);
      }
    }
    return pairedTermData;
  }

  public String getSynonymsAsString(List<Synonym> synonyms) {
    return decodeSpecialChar(
        synonyms.stream()
            .filter(
                synonym ->
                    TERM_GROUP_SYNONYM.equals(synonym.getTermGroup())
                        && TERM_SOURCE_CDISC.equals(synonym.getTermSource()))
            .map(Synonym::getTermName)
            .sorted(new SortComparatorV2())
            .collect(Collectors.joining(";")));
  }

  public TargetTermPair searchPairedTargetTerm(
      String memberCode, String cdiscSyTermName, String nciAb) {
    List<Synonym> targetSynonyms =
        this.conceptMap.values().stream()
            .flatMap(concept -> concept.getSynonyms().stream())
            .filter(getTermPredicate(TERM_GROUP_AB, TERM_SOURCE_NCI, nciAb))
            .filter(getTermPredicate(TERM_GROUP_SYNONYM, TERM_SOURCE_CDISC, cdiscSyTermName))
            .collect(Collectors.toList());
    if (!targetSynonyms.isEmpty()) {
      String targetConceptCode = targetSynonyms.get(0).getCode();
      String cdiscSynonym =
          targetSynonyms.stream()
              .filter(getTermPredicate(TERM_GROUP_SYNONYM, TERM_SOURCE_CDISC))
              .map(Synonym::getTermName)
              .findFirst()
              .orElse("");
      String cdiscPreferredTerm =
          this.conceptMap.get(memberCode).getSynonyms().stream()
              .filter(
                  getSynonymSourceCodePredicate(
                      TERM_GROUP_PREFERRED_TERM, TERM_SOURCE_CDISC, nciAb))
              .map(Synonym::getTermName)
              .findFirst()
              .orElse("");
      return TargetTermPair.builder()
          .cdiscPreferredTerm(cdiscPreferredTerm)
          .cdiscSynonym(cdiscSynonym)
          .conceptCode(targetConceptCode)
          .build();
    }
    return null;
  }

  public static void main(String[] args) {
    if (args == null || args.length != 4) {
      System.out.println(
          "Command line parameters: (1) ThesaurusInferred_forTS.owl  (2): Root concept code (e.g., C77526) (3): Output directory (4): Publication Date");
      System.exit(1);
    }

    String owlFile = args[0];
    String root = args[1];
    Path outputDirectory = Paths.get(args[2]);
    String publicationDate = args[3];
    LocalDate.parse(publicationDate);
    CDISCPairingV2 cdiscPairing =
        new CDISCPairingV2(new File(owlFile), outputDirectory, publicationDate);
    cdiscPairing.run(root);
  }
}
