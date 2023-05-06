package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.model.PairedTerm;
import gov.nih.nci.evs.cdisc.report.model.PairedTermData;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.report.utils.PairedTermDataComparator;
import gov.nih.nci.evs.cdisc.report.utils.ReportUtils;
import gov.nih.nci.evs.cdisc.report.utils.XLSXFormatter;
import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import gov.nih.nci.evs.cdisc.thesaurus.owl.ThesaurusOwlReader;
import gov.nih.nci.evs.cdisc.thesaurus.utils.ThesaurusUtils;
import gov.nih.nci.evs.restapi.util.ExcelReadWriteUtils;
import gov.nih.nci.evs.restapi.util.SortUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static gov.nih.nci.evs.cdisc.thesaurus.utils.ThesaurusUtils.*;

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
 */

/**
 * @author EVS Team
 * @version 1.0
 *     <p>Modification history: Initial implementation kim.ong@nih.gov
 */
@Slf4j
public class CDISCPairingV2 {
  static String METADATA_HEADER =
      "Code|Codelist Code|Codelist Extensible (Yes/No)|Codelist Name|CDISC Submission Value|CDISC Synonym(s)|CDISC Definition|NCI Preferred Term";
  static String PAIRED_TERMS_HEADER =
      "Code|TESTCD/PARMCD Codelist Code|TESTCD/PARMCD Codelist Name|TESTCD/PARMCD CDISC Submission Value Code|TEST/PARM Codelist Code|TEST/PARM Codelist Name|TEST/PARM CDISC Submission Value Name|CDISC Synonym(s)|CDISC Definition|NCI Preferred Term";

  private final ThesaurusUtils thesaurusUtils;
  private boolean isGlossary;
  private List<Concept> focusedCodes = new ArrayList<>();
  private Map<String, List<Synonym>> synonymMap = null;

  private Map<String, List<PairedTerm>> pairedSourceTermData = null;
  private List<Concept> conceptsWithNciAb = null;
  private List<PairedTermData> pairedTermData = null;

  private final Path outputDirectory;
  private final String publicationDate;
  private final Map<String, Concept> conceptMap;

  public CDISCPairingV2(File owlFile, Path outputDirectory, String publicationDate) {
    conceptMap = new ThesaurusOwlReader(owlFile).createConceptMap();
    thesaurusUtils = new ThesaurusUtils(conceptMap);
    this.outputDirectory = outputDirectory;
    this.publicationDate = publicationDate;
  }

  private List<Concept> getFocusedConceptsWithSourceCode() {
    return focusedCodes.stream()
        .filter(concept -> concept.getSynonyms().stream().anyMatch(this::hasSourceCode))
        .collect(Collectors.toList());
  }

  private boolean hasSourceCode(Synonym synonym) {
    return ThesaurusUtils.TERM_GROUP_PREFERRED_TERM.equals(synonym.getTermGroup())
        && ThesaurusUtils.TERM_SOURCE_CDISC.equals(synonym.getTermSource())
        && org.apache.commons.lang3.StringUtils.isNotBlank(synonym.getSourceCode());
  }

  public List<Concept> getConceptsWithNciAb() {
    log.info("Finding concepts with NCI and AB");
    List<Concept> conceptsWithNCIAB = new ArrayList<>();
    for (Map.Entry<String, List<Synonym>> synonyms : synonymMap.entrySet()) {
      if (synonyms.getValue().stream().anyMatch(ThesaurusUtils::hasAbAndNci)) {
        conceptsWithNCIAB.add(conceptMap.get(synonyms.getKey()));
      }
    }
    log.debug("Found {} concepts with NCI and AB", conceptsWithNCIAB.size());
    return conceptsWithNCIAB;
  }

  public PairedTerm searchPairedSourceTerm(Concept memberConcept, String submissionValueCode) {
    log.trace("Searching for pair source term. concept:{}", memberConcept.getCode());
    for (Concept conceptWithNCIAB : conceptsWithNciAb) {
      List<Synonym> synonyms = conceptWithNCIAB.getSynonyms();
      String cdiscSynonym = null;
      boolean nciAbMatched = false;
      for (Synonym synonym : synonyms) {
        if (synonym.getTermName().equals(submissionValueCode) && hasAbAndNci(synonym)) {
          nciAbMatched = true;
        }
        if (hasSyAndCdisc(synonym)) {
          cdiscSynonym = synonym.getTermName();
        }
        if (nciAbMatched && cdiscSynonym != null) {
          // find CDISC PT matching source code: cd_code
          String cdiscPreferredTerm =
              findTermNameMatchingCDISCPTSourceCode(memberConcept.getCode(), submissionValueCode);
          // For whatever reason, the CDISC SYN terms were escaped for built-in entities in the v1
          // report. So retaining that functionality
          return new PairedTerm(
              memberConcept.getCode(),
              synonym.getCode(),
              StringEscapeUtils.escapeXml11(cdiscSynonym),
              cdiscPreferredTerm,
              submissionValueCode);
        }
      }
    }
    return null;
  }

  public List<PairedTerm> getPairedSourceTermData(Concept concept) {
    log.trace("Getting paired source term data for {}", concept.getCode());
    List<PairedTerm> pairedSourceTerms = new ArrayList<>();
    List<Synonym> synonyms = synonymMap.get(concept.getCode());
    List<String> submissionValueCodes = getSubmissionValueCodes(synonyms, concept.getCode());
    for (String submissionValueCode : submissionValueCodes) {
      PairedTerm pairedSourceTerm = searchPairedSourceTerm(concept, submissionValueCode);
      if (pairedSourceTerm != null) {
        pairedSourceTerms.add(pairedSourceTerm);
      }
    }
    log.trace(
        "Found {} paired source term data for {}", pairedSourceTerms.size(), concept.getCode());
    return pairedSourceTerms;
  }

  public Map<String, List<PairedTerm>> generatePairedSourceTermData() {
    log.info("Generating pair source terms");
    List<Concept> sourceCodeFocusedConcepts = getFocusedConceptsWithSourceCode();
    log.trace("Found {} focused codes with source code", sourceCodeFocusedConcepts.size());
    Map<String, List<PairedTerm>> pairedSourceTermMap = new HashMap<>();
    for (Concept concept : sourceCodeFocusedConcepts) {
      List<PairedTerm> pairedSourceTerms = getPairedSourceTermData(concept);
      if (pairedSourceTerms.size() > 0) {
        log.trace("Found {} pair source terms for {}", concept.getCode(), pairedSourceTerms.size());
        pairedSourceTermMap.put(concept.getCode(), pairedSourceTerms);
      }
    }
    return pairedSourceTermMap;
  }

  public PairedTerm searchPairedTargetTerm(
      String memberCode, String cdiscSyTermName, String nciAb) {
    for (Concept conceptWithNciAndAb : conceptsWithNciAb) {
      List<Synonym> synonyms = synonymMap.get(conceptWithNciAndAb.getCode());
      String cdiscSynonym = null;
      boolean nciAbMatched = false;
      boolean cdiscSynonymMatched = false;
      String termName;
      for (Synonym syn : synonyms) {
        termName = syn.getTermName();
        if (hasAbAndNci(syn) && nciAb.equals(termName)) {
          nciAbMatched = true;
        }
        if (hasSyAndCdisc(syn)) {
          cdiscSynonym = StringEscapeUtils.escapeXml11(syn.getTermName());
          if (cdiscSynonym.equals(cdiscSyTermName)) {
            cdiscSynonymMatched = true;
          }
        }
      }
      if (nciAbMatched && cdiscSynonymMatched) {
        String cdiscPreferredTerm = findTermNameMatchingCDISCPTSourceCode(memberCode, nciAb);
        return new PairedTerm(
            conceptWithNciAndAb.getCode(), null, cdiscSynonym, cdiscPreferredTerm, null);
      }
    }
    return null;
  }

  public String findTermNameMatchingCDISCPTSourceCode(String memberCode, String sourceCode) {
    return synonymMap.get(memberCode).stream()
        .filter(
            synonym ->
                ThesaurusUtils.hasPtAndCdisc(synonym)
                    && synonym.getSourceCode() != null
                    && synonym.getSourceCode().equals(sourceCode))
        .map(Synonym::getTermName)
        .findFirst()
        .orElse(null);
  }

  public List<PairedTermData> generatePairedTermData() {
    log.info("Generating paired term data");
    List<PairedTermData> pairedTerms = new ArrayList<>();
    for (Map.Entry<String, List<PairedTerm>> entry : pairedSourceTermData.entrySet()) {
      String code = entry.getKey();
      Concept concept = conceptMap.get(code);
      List<PairedTerm> pairedSourceTerms = entry.getValue();
      for (PairedTerm pairedSourceTerm : pairedSourceTerms) {
        String memberCode = pairedSourceTerm.getMemberCode();
        Concept memberConcept = conceptMap.get(memberCode);
        String sourceCode = pairedSourceTerm.getCode();
        if (sourceCode != null) {
          String name0 = pairedSourceTerm.getCdiscSynonym();
          String name = makeCdiscSynonymReplacements(name0);
          String cdiscPreferredTerm = pairedSourceTerm.getCdiscPreferredTerm();
          String submissionValueCode = pairedSourceTerm.getSubmissionValueCode();
          if (submissionValueCode.contains("CD")) {
            submissionValueCode = submissionValueCode.replace("CD", "");
          }
          String synonyms = getSynonyms(code);
          if (org.apache.commons.lang3.StringUtils.isBlank(synonyms)) {
            continue;
          }
          synonyms = decodeSpecialChar(synonyms);
          String cdiscDefinition = thesaurusUtils.getCdiscDefinition(concept, isGlossary);
          String preferredTerm = concept.getPreferredName();
          PairedTerm targetData = searchPairedTargetTerm(memberCode, name, submissionValueCode);
          if (targetData != null) {
            PairedTermData pairedTermData =
                new PairedTermData(
                    memberCode,
                    sourceCode,
                    name0,
                    cdiscPreferredTerm,
                    targetData,
                    synonyms,
                    cdiscDefinition,
                    preferredTerm);
            if (!memberConcept.isRetired()) {
              pairedTerms.add(pairedTermData);
            }
          }
        }
      }
    }
    return pairedTerms;
  }

  private String makeCdiscSynonymReplacements(String cdiscSynonym) {
    String replaced = cdiscSynonym.replace("Parameter Code", "Parameter Long Name");
    replaced = replaced.replace("Test Code", "Test Name");
    replaced = replaced.replace("Short Name", "Long Name");
    return replaced.replace("Parameters Code", "Parameters");
  }

  public ReportDetail run(String root) {
    log.info("Starting pairing report. Root concept:{}", root);
    String terminology;
    Concept rootConcept = conceptMap.get(root);
    if (rootConcept == null
        || org.apache.commons.lang3.StringUtils.isBlank(rootConcept.getPreferredName())) {
      throw new RuntimeException(
          String.format("Unable to determine terminology. root code:%s", root));
    }
    isGlossary = thesaurusUtils.isGlossaryConcept(rootConcept);
    focusedCodes = thesaurusUtils.createFocusedConcepts(rootConcept);
    terminology = getTerminology(rootConcept.getPreferredName());
    synonymMap = thesaurusUtils.createSynonymMap(focusedCodes);
    String excelFileName = null;
    try {
      conceptsWithNciAb = getConceptsWithNciAb();
      pairedSourceTermData = generatePairedSourceTermData();
      pairedTermData = generatePairedTermData();
      pairedTermData.sort(new PairedTermDataComparator());
      String outputFile = writeTextFile();
      String metadataFile = generateMetadata();
      String blankFile = writeBlankReadmeFile();

      Vector<String> textFiles = new Vector<>();
      textFiles.add(blankFile);
      textFiles.add(metadataFile);
      textFiles.add(outputFile);

      excelFileName = writeExcelFile(terminology, textFiles);

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return ReportDetail.builder()
        .code(root)
        .label(terminology)
        .reports(Collections.singletonMap(ReportEnum.PAIRING_EXCEL, excelFileName))
        .build();
  }

  private String writeTextFile() throws IOException {
    log.info("Generating pairing text report");
    String outputFile = getPairedTermDataTextFileName(this.outputDirectory);
    try (FileWriter textFileWriter = new FileWriter(outputFile)) {
      List<String> lines =
          pairedTermData.stream().map(PairedTermData::toLine).collect(Collectors.toList());
      // Add header
      lines.add(0, PAIRED_TERMS_HEADER);
      IOUtils.writeLines(lines, System.lineSeparator(), textFileWriter);
    }
    return outputFile;
  }

  public String getPairedTermDataTextFileName(Path outputDirectory) {
    String pairedTermDataTextFileName = "pairedTermData_v2.txt";
    return outputDirectory.resolve(pairedTermDataTextFileName).toString();
  }

  private String writeMetadataFile(List<String> metadataLines) throws IOException {
    log.info("Generating pairing metadata text file");
    String outputfile = getMetadataTextFileName(outputDirectory);
    try (FileWriter metadataWriter = new FileWriter(outputfile)) {
      IOUtils.writeLines(metadataLines, System.lineSeparator(), metadataWriter);
    }
    return outputfile;
  }

  private String getMetadataTextFileName(Path outputDirectory) {
    return outputDirectory.resolve("metadata_v2.txt").toString();
  }

  private String writeBlankReadmeFile() throws IOException {
    log.info("Generating pairing readme text file");
    String readmeTextFileName = getReadmeTextFileName(this.outputDirectory);
    new File(readmeTextFileName).createNewFile();
    return readmeTextFileName;
  }

  private String getReadmeTextFileName(Path outputDirectory) {
    return outputDirectory.resolve("readme_v2.txt").toString();
  }

  private String writeExcelFile(String terminology, Vector<String> textFiles) throws IOException {
    log.info("Generating pairing excel report");
    String excelFileName = getExcelFileName(terminology);

    Vector<String> sheetNames = new Vector<>();
    sheetNames.add("ReadMe");
    sheetNames.add(String.format("%s Paired Codelist Metadata", terminology));
    sheetNames.add(String.format("%s Paired Terms", terminology));
    try {
      ExcelReadWriteUtils.writeXLSXFile(excelFileName, textFiles, sheetNames, '|');
    } finally {
      log.info("Cleaning up intermediate text files");
      // textFiles.forEach(fileName -> new File(fileName).delete());
    }
    XLSXFormatter.reformat(excelFileName, excelFileName);
    return excelFileName;
  }

  private String getExcelFileName(String terminology) {
    String excelFileName =
        new StringBuilder(terminology)
            .append("_paired_view_")
            .append(publicationDate.replace("-", "_")) // To retain the existing file name
            .append("_v2.xlsx")
            .toString();
    return ReportUtils.getOutputPath(outputDirectory, terminology)
        .resolve(excelFileName)
        .toString();
  }

  public List<String> getSubmissionValueCodes(List<Synonym> synonyms, String code) {
    log.trace("Getting submission value codes for {}", code);
    List<String> submissionValueCodes = new ArrayList<>();
    List<String> sourceCodes =
        synonyms.stream()
            .filter(this::hasSourceCode)
            .map(Synonym::getSourceCode)
            .collect(Collectors.toList());
    for (String sourceCode : sourceCodes) {
      if (sourceCode.contains("CD")) {
        String replacedSourceCode = sourceCode.replace("CD", "");
        if (sourceCodes.contains(replacedSourceCode)) {
          submissionValueCodes.add(sourceCode);
        }
      }
    }
    return submissionValueCodes;
  }

  public String getSynonyms(String code) {
    List<String> strSynonyms = new ArrayList<>();
    List<Synonym> synonyms = synonymMap.get(code);
    if (synonyms != null) {
      // Collecting to Set to remove dups
      strSynonyms =
          synonyms.stream()
              .filter(ThesaurusUtils::hasSyAndCdisc)
              .map(Synonym::getTermName)
              .distinct()
              .collect(Collectors.toList());
      new SortUtils().quickSort(strSynonyms);
    }
    return strSynonyms.size() > 0 ? String.join("; ", strSynonyms) : "";
  }

  public String generateMetadata() throws IOException {
    log.info("Generating metadata");
    List<String> metadataLines = new ArrayList<>();
    metadataLines.add(METADATA_HEADER);
    for (PairedTermData pairedTermData : pairedTermData) {
      String code = pairedTermData.getMemberCode();
      Concept concept = conceptMap.get(code);
      String codeListCode = pairedTermData.getSourceCode();
      String extensible = thesaurusUtils.getExtensibleString(codeListCode);
      String codeListName = pairedTermData.getName();
      String submissionValue = pairedTermData.getCdiscPreferredTerm();
      String synonyms = decodeSpecialChar(getSynonyms(code));
      String definition = thesaurusUtils.getCdiscDefinition(concept, isGlossary);
      String preferredTerm = concept.getPreferredName();
      String metadataLine =
          String.join(
              "|",
              code,
              codeListCode,
              extensible,
              codeListName,
              submissionValue,
              synonyms,
              definition,
              preferredTerm);
      metadataLines.add(decodeSpecialChar(metadataLine));

      PairedTerm targetPairedTerm = pairedTermData.getTargetTerm();
      codeListCode = targetPairedTerm.getMemberCode();
      extensible = thesaurusUtils.getExtensibleString(codeListCode);
      codeListName = targetPairedTerm.getCdiscSynonym();
      submissionValue = targetPairedTerm.getCdiscPreferredTerm();
      metadataLine =
          String.join(
              "|",
              code,
              codeListCode,
              extensible,
              codeListName,
              submissionValue,
              synonyms,
              definition,
              preferredTerm);
      metadataLines.add(decodeSpecialChar(metadataLine));
    }
    return writeMetadataFile(metadataLines);
  }

  public static void main(String[] args) {
    if(args.length < 3){
      log.error("Wrong number of parameters. Expected {}. Got {}", 3, args.length);
      log.error("Usage: CDISCPairingV2 <path to OWL file> <root concept of report>");
      log.error(
          "Example: CDISCPairingV2 /tmp/Thesaurus-230320-23.03c_fixed.owl C66830 /tmp/pairing_reports");
      System.exit(1);
    }
    String owlFile = args[0];
    String root = args[1];
    Path outputDirectory = Paths.get(args[2]);
    CDISCPairingV2 cdiscPairing =
        new CDISCPairingV2(new File(owlFile), outputDirectory, LocalDate.now().toString());
    cdiscPairing.run(root);
  }
}
