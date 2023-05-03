package gov.nih.nci.evs.cdisc.report;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import gov.nih.nci.evs.cdisc.report.model.ChangeV2;
import gov.nih.nci.evs.cdisc.report.model.CodeListV2;
import gov.nih.nci.evs.cdisc.report.model.ElementV2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getSynonyms;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class ChangesReportV2 {
  private final Map<CodeListV2, List<ElementV2>> newMap;
  private final Map<CodeListV2, List<ElementV2>> oldMap;
  private final String releaseDate;
  private final PrintWriter outputWriter;
  private final String requestCode = "";
  private final List<ChangeV2> changes = new ArrayList<>();

  public ChangesReportV2(String newReport, String oldReport, String date, String outputDirectory)
      throws IOException {
    newMap = parseCdiscTextReport(newReport);
    oldMap = parseCdiscTextReport(oldReport);
    releaseDate = date;
    outputWriter = new PrintWriter(outputDirectory);
  }

  public Map<CodeListV2, List<ElementV2>> parseCdiscTextReport(String filename) throws IOException {
    log.info("parsing cdisc report {}", filename);
    Map<CodeListV2, List<ElementV2>> codeListMap = new HashMap<>();
    try (InputStream textReportInputStream = new FileInputStream(filename)) {
      List<String> lines = IOUtils.readLines(textReportInputStream, Charset.defaultCharset());
      for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
        String line = lines.get(lineNum).trim();
        String[] tokens = line.split("\t");
        if (tokens.length == 8) {
          String code = tokens[0];
          String codeListCode = tokens[1];
          String extensible = tokens[2];
          String codeListName = tokens[3];
          String submissionValue = tokens[4];
          String strSynonyms = tokens[5];
          String definition = tokens[6];
          String preferredTerm = tokens[7];
          if (isNotBlank(code) && isBlank(codeListCode)) {
            CodeListV2 cl =
                new CodeListV2(
                    code,
                    extensible,
                    codeListName,
                    submissionValue,
                    getSynonyms(strSynonyms),
                    definition,
                    preferredTerm);
            codeListMap.put(cl, null);
          } else {
            ElementV2 element =
                new ElementV2(
                    code,
                    codeListCode,
                    codeListName,
                    submissionValue,
                    getSynonyms(strSynonyms),
                    definition,
                    preferredTerm);
            populateCodeListMap(codeListMap, element);
          }
        } else {
          log.error("Unable to read {}: line {}", filename, lineNum + 1);
        }
      }
    }
    return codeListMap;
  }

  private void populateCodeListMap(
      Map<CodeListV2, List<ElementV2>> codeListMap, ElementV2 element) {
    log.trace(
        "Adding element to codeListMap. codeListCode:{} elementCode:{}",
        element.getCodeListCode(),
        element.getCode());
    boolean found = false;
    for (CodeListV2 codeList : codeListMap.keySet()) {
      if (codeList.getCode().equals(element.getCodeListCode())) {
        found = true;
        List<ElementV2> elements = codeListMap.get(codeList);
        if (elements == null) {
          elements = new ArrayList<>();
        }
        elements.add(element);
        codeListMap.put(codeList, elements);
        break;
      }
    }
    if (!found) {
      log.error(
          "Unable to find the codeList {} for element {}",
          element.getCode(),
          element.getCodeListCode());
    }
  }

  public void getChanges() {
    log.info("Calculating differences between current and previous reports");
    MapDifference<CodeListV2, List<ElementV2>> difference = Maps.difference(oldMap, newMap);
    Map<CodeListV2, List<ElementV2>> common = difference.entriesInCommon();
    log.info("Found {} codeLists that are the same", common.size());
    // The codeList is common, but the elements are different
    Map<CodeListV2, MapDifference.ValueDifference<List<ElementV2>>> entriesDiffering =
        difference.entriesDiffering();
    log.info(
        "Found {} codeLists that are common but have differing element codes",
        entriesDiffering.size());
    for (CodeListV2 codeList : common.keySet()) {
      log.trace("Checking for updates of codeList. code:{}", codeList.getCode());
      CodeListV2 oldCodeList =
          oldMap.keySet().stream().filter(ol -> ol.equals(codeList)).findFirst().get();
      CodeListV2 newCodeList =
          newMap.keySet().stream().filter(nl -> nl.equals(codeList)).findFirst().get();
      compareCodeLists(oldCodeList, newCodeList);
      List<ElementV2> oldElements = oldMap.get(codeList);
      List<ElementV2> newElements = newMap.get(codeList);
      compareElements(oldElements, newElements, newCodeList.getSubValue());
    }

    for (CodeListV2 codeList : entriesDiffering.keySet()) {
      List<ElementV2> oldElements = oldMap.get(codeList);
      List<ElementV2> newElements = newMap.get(codeList);
      compareElements(oldElements, newElements, codeList.getSubValue());
    }

    Map<CodeListV2, List<ElementV2>> addedCodes = difference.entriesOnlyOnRight();
    log.info("{} codeLists that are new", addedCodes.size());
    for (Map.Entry<CodeListV2, List<ElementV2>> addedCode : addedCodes.entrySet()) {
      getChangesForNewCodeList(addedCode.getKey(), addedCode.getValue());
    }
    log.info("{} codeLists that are removed", addedCodes.size());
    Map<CodeListV2, List<ElementV2>> removedCodes = difference.entriesOnlyOnLeft();
    for (Map.Entry<CodeListV2, List<ElementV2>> removedCode : removedCodes.entrySet()) {
      getChangesForRemovedCodeList(removedCode.getKey(), removedCode.getValue());
    }
  }

  public void getChangesForNewCodeList(CodeListV2 newCodeList, List<ElementV2> newElements) {
    changes.add(
        new ChangeV2(
            releaseDate,
            requestCode,
            "Add",
            newCodeList.getCode(),
            "CDISC Codelist",
            newCodeList.getSubValue(),
            newCodeList.getName(),
            "Addition of new codelist",
            "- - -",
            newCodeList.getSubValue()));
    if (newElements != null) {
      for (ElementV2 e : newElements) {
        changes.add(
            new ChangeV2(
                releaseDate,
                requestCode,
                "Add",
                e.getCode(),
                "Term",
                newCodeList.getSubValue(),
                e.getName(),
                "Add new term to new codelist",
                "- - -",
                e.getSubValue()));
      }
    } else {
      log.info("No Terms associated with new codeList {}", newCodeList.getName());
    }
  }

  public void getChangesForRemovedCodeList(CodeListV2 oldCodeList, List<ElementV2> oldElements) {
    changes.add(
        new ChangeV2(
            releaseDate,
            requestCode,
            "Remove",
            oldCodeList.getCode(),
            "CDISC Codelist",
            oldCodeList.getSubValue(),
            oldCodeList.getName(),
            "Retire codelist",
            oldCodeList.getSubValue(),
            "- - -"));
    if (oldElements != null) {
      for (ElementV2 e : oldElements) {
        changes.add(
            new ChangeV2(
                releaseDate,
                requestCode,
                "Remove",
                e.getCode(),
                "Term",
                oldCodeList.getSubValue(),
                e.getName(),
                "Remove term from retired codelist",
                e.getSubValue(),
                "- - -"));
      }
    } else {
      log.info("No Terms associated with codeList {}", oldCodeList.getName());
    }
  }

  public void compareElements(
      List<ElementV2> oldElements, List<ElementV2> newElements, String codeListShortName) {
    if (oldElements == null && newElements == null) {
      return;
    }
    if (oldElements != null && newElements != null) {
      Collection<ElementV2> common = CollectionUtils.intersection(oldElements, newElements);
      for (ElementV2 commonElement : common) {
        ElementV2 oldElement = oldElements.get(oldElements.indexOf(commonElement));
        ElementV2 newElement = newElements.get(newElements.indexOf(commonElement));
        diffElements(oldElement, newElement, codeListShortName);
      }
    }
    Collection<ElementV2> existsInOld =
        newElements != null ? CollectionUtils.subtract(oldElements, newElements) : oldElements;
    for (ElementV2 oldElement : existsInOld) {
      getChangesForRemoveTermFromExistingCodeList(oldElement, codeListShortName);
    }

    Collection<ElementV2> existsInNew =
        oldElements != null ? CollectionUtils.subtract(newElements, oldElements) : newElements;
    for (ElementV2 newElement : existsInNew) {
      getChangesForAddingTermToExistingCodeList(newElement, codeListShortName);
    }
  }

  public void getChangesForRemoveTermFromExistingCodeList(
      ElementV2 element, String codeListShortName) {
    changes.add(
        new ChangeV2(
            releaseDate,
            requestCode,
            "Remove",
            element.getCode(),
            "Term",
            codeListShortName,
            element.getName(),
            "Remove term entirely from codelist",
            element.getSubValue(),
            "- - -"));
  }

  public void getChangesForAddingTermToExistingCodeList(
      ElementV2 element, String codeListShortName) {
    changes.add(
        new ChangeV2(
            releaseDate,
            requestCode,
            "Add",
            element.getCode(),
            "Term",
            codeListShortName,
            element.getName(),
            "Add new term to existing codelist",
            "- - -",
            element.getSubValue()));
  }

  private void evaluateCodeListChange(
      String changeType,
      String changedField,
      Function<CodeListV2, String> function,
      CodeListV2 oldCodeList,
      CodeListV2 newCodeList) {
    String oldValue = function.apply(oldCodeList);
    String newValue = function.apply(newCodeList);
    if (!oldValue.equals(newValue)) {
      changes.add(
          new ChangeV2(
              releaseDate,
              requestCode,
              changeType,
              newCodeList.getCode(),
              changedField,
              newCodeList.getSubValue(),
              newCodeList.getName(),
              String.join(" ", changeType, changedField),
              oldValue,
              newValue));
    }
  }

  private void evaluateSynonymChanges(
      String[] oldSynonyms,
      String[] newSynonyms,
      String code,
      String submissionValue,
      String name) {
    Arrays.sort(oldSynonyms);
    Arrays.sort(newSynonyms);
    for (String newSynonym : newSynonyms) {
      boolean found = false;
      for (String oldSynonym : oldSynonyms) {
        if (newSynonym.equals(oldSynonym)) {
          found = true;
          break;
        }
      }
      if (!found) {
        if (StringUtils.isNotBlank(newSynonym)) {
          changes.add(
              new ChangeV2(
                  releaseDate,
                  requestCode,
                  "Update",
                  code,
                  "CDISC Synonym",
                  submissionValue,
                  name,
                  "Add new CDISC Synonym",
                  "- - -",
                  newSynonym));
        }
      }
    }

    // and backwards
    for (String oldSynonym : oldSynonyms) {
      boolean found = false;
      for (String newSynonym : newSynonyms) {
        if (newSynonym.equals(oldSynonym)) {
          found = true;
          break;
        }
      }
      if (!found) {
        if (StringUtils.isNotBlank(oldSynonym)) {
          changes.add(
              new ChangeV2(
                  releaseDate,
                  requestCode,
                  "Update",
                  code,
                  "CDISC Synonym",
                  submissionValue,
                  name,
                  "Remove CDISC Synonym",
                  oldSynonym,
                  "- - -"));
        }
      }
    }
  }

  public void compareCodeLists(CodeListV2 oldCodeList, CodeListV2 newCodeList) {
    evaluateCodeListChange(
        "Update", "CDISC Extensible List", CodeListV2::getExtensible, oldCodeList, newCodeList);
    evaluateCodeListChange(
        "Update", "CDISC Codelist Name", CodeListV2::getName, oldCodeList, newCodeList);
    evaluateCodeListChange(
        "Update", "CDISC Submission Value", CodeListV2::getSubValue, oldCodeList, newCodeList);
    evaluateCodeListChange(
        "Update", "CDISC Definition", CodeListV2::getDefinition, oldCodeList, newCodeList);
    evaluateCodeListChange(
        "Update", "NCI Preferred Term", CodeListV2::getPreferredTerm, oldCodeList, newCodeList);
    evaluateSynonymChanges(
        oldCodeList.getSynonyms(),
        newCodeList.getSynonyms(),
        newCodeList.getCode(),
        newCodeList.getSubValue(),
        newCodeList.getName());
  }

  private void evaluateElementChange(
      String changeType,
      String changedField,
      Function<ElementV2, String> function,
      String clShortName,
      ElementV2 oldElement,
      ElementV2 newElement) {
    String oldValue = function.apply(oldElement);
    String newValue = function.apply(newElement);
    if (!oldValue.equals(newValue)) {
      changes.add(
          new ChangeV2(
              releaseDate,
              requestCode,
              changeType,
              newElement.getCode(),
              changedField,
              clShortName,
              newElement.getName(),
              String.join(" ", changeType, changedField),
              oldValue,
              newValue));
    }
  }

  public void diffElements(ElementV2 oldElement, ElementV2 newElement, String codeListShortName) {
    evaluateElementChange(
        "Update",
        "CDISC Codelist Name",
        ElementV2::getName,
        codeListShortName,
        oldElement,
        newElement);
    evaluateElementChange(
        "Update",
        "CDISC Submission Value",
        ElementV2::getSubValue,
        codeListShortName,
        oldElement,
        newElement);
    evaluateElementChange(
        "Update",
        "CDISC Definition",
        ElementV2::getDefinition,
        codeListShortName,
        oldElement,
        newElement);
    evaluateElementChange(
        "Update",
        "NCI Preferred Term",
        ElementV2::getPreferredTerm,
        codeListShortName,
        oldElement,
        newElement);
    evaluateSynonymChanges(
        oldElement.getSynonyms(),
        newElement.getSynonyms(),
        newElement.getCode(),
        codeListShortName,
        newElement.getName());
  }

  public void print() {
    changes.sort(Comparator.comparingInt(ChangeV2::getNoCCode));
    changes.sort(Comparator.comparing(ChangeV2::getCodeListShortName));
    try (outputWriter) {
      outputWriter.println(
          String.join(
              "\t",
              "Release Date",
              "Request Code",
              "Change Type",
              "NCI Code",
              "CDISC Term Type",
              "CDISC Codelist (Short Name)",
              "CDISC Codelist (Long Name)",
              "Change Summary",
              "Original",
              "New",
              "Change Implementation Instructions"));
      for (ChangeV2 c : changes) {
        outputWriter.println(c.toLine());
      }
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length < 4) {
      log.error("Wrong number of parameters. Expected {}. Got {}", 4, args.length);
      log.error(
          "Usage: ChangesReportV2 <new report text file> <previous report text file> <publication date> <output filename>");
      log.error(
          "Example: ChangesReportV2 \"SDTM Terminology.txt\" \"SDTM Terminology 2014-03-28.txt\" \"2014-06-27\" \"D:\\data\\SDTM Changes.txt\"");
      System.exit(1);
    }
    ChangesReportV2 report = new ChangesReportV2(args[0], args[1], args[2], args[3]);
    report.getChanges();
    report.print();
  }
}
