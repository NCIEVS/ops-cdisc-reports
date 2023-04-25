package gov.nih.nci.evs.cdisc.thesaurus.utils;

import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.thesaurus.model.AlternativeDefinition;
import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ThesaurusUtils {
  private final Map<String, Concept> conceptMap;
  public static final String TERM_SOURCE_CDISC = "CDISC";
  public static final String TERM_SOURCE_NCI = "NCI";
  public static final String TERM_SOURCE_CDISC_GLOSSARY = "CDISC-GLOSS";
  public static final String TERM_GROUP_SYNONYM = "SY";
  public static final String TERM_GROUP_PREFERRED_TERM = "PT";
  public static final String TERM_GROUP_AB = "AB";

  public ThesaurusUtils(Map<String, Concept> conceptMap) {
    this.conceptMap = conceptMap;
  }

  public void populateCodeListCodes(Concept rootConcept, List<Concept> allDescendantCodes) {
    List<Concept> childListCodes =
        this.conceptMap.values().stream()
            .filter(concept -> concept.getParents().contains(rootConcept.getCode()))
            .collect(Collectors.toList());
    allDescendantCodes.addAll(childListCodes);
    for (Concept concept : childListCodes) {
      populateCodeListCodes(concept, allDescendantCodes);
    }
  }

  public List<Concept> getChildren(Concept rootConcept) {
    return this.conceptMap.values().stream()
        .filter(concept -> concept.getParents().contains(rootConcept.getCode()))
        .collect(Collectors.toList());
  }

  public List<Concept> createFocusedConcepts(Concept rootConcept) {
    log.info("Getting focused concepts for concept:{}", rootConcept);
    List<Concept> focusedConcepts = new ArrayList<>();
    List<Concept> children = getChildren(rootConcept);
    focusedConcepts.addAll(children);
    // Adding subsets of root concept
    populateCodeInSubsets(Collections.singletonList(rootConcept));
    populateCodeInSubsets(children);
//    focusedConcepts.addAll(rootConcept.getCodeInSubsets());
    for (Concept concept : children) {
      focusedConcepts.addAll(concept.getCodeInSubsets());
    }
    log.info("Found {} focused concepts", focusedConcepts.size());
    return focusedConcepts;
  }

  private void populateCodeInSubsets(List<Concept> concepts) {
    for (Concept concept : concepts) {
      List<Concept> subsets = this.conceptMap.values().stream()
              .filter(
                      subsetConcept -> subsetConcept.getSubsetCodes().contains(concept.getCode()))
              .collect(Collectors.toList());
      concept
          .getCodeInSubsets()
          .addAll(subsets);
    }
  }

  public Map<String, List<Synonym>> createSynonymMap(List<Concept> focusedConcepts) {
    log.info("Creating synonym map of all focused concepts");
    Map<String, List<Synonym>> synonymMap = new HashMap<>();
    if (focusedConcepts == null) {
      return synonymMap;
    }
    return focusedConcepts.stream().collect(Collectors.toSet()).stream()
        .collect(Collectors.toMap(Concept::getCode, Concept::getSynonyms));
  }

  public boolean isGlossaryConcept(Concept concept) {
    return concept.getLabel() != null && concept.getLabel().contains("Glossary");
  }

  public String getCdiscDefinition(Concept concept, boolean glossaryRootConcept) {
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

  public static boolean hasSyAndCdisc(Synonym synonym) {
    return TERM_GROUP_SYNONYM.equals(synonym.getTermGroup())
        && TERM_SOURCE_CDISC.equals(synonym.getTermSource());
  }

  public static boolean hasPtAndCdisc(Synonym synonym) {
    return TERM_GROUP_PREFERRED_TERM.equals(synonym.getTermGroup())
        && TERM_SOURCE_CDISC.equals(synonym.getTermSource());
  }

  public static boolean hasAbAndNci(Synonym synonym) {
    return TERM_GROUP_AB.equals(synonym.getTermGroup())
        && TERM_SOURCE_NCI.equals(synonym.getTermSource());
  }

  public static String decodeSpecialChar(String line) {
    line = line.replaceAll("&apos;", "'");
    line = line.replaceAll("&amp;", "&");
    line = line.replaceAll("&lt;", "<");
    line = line.replaceAll("&gt;", ">");
    line = line.replaceAll("&quot;", "\"");
    return line;
  }

  public String getExtensibleString(String code) {
    Concept concept = conceptMap.get(code);
    if (concept == null) {
      return "";
    }
    Boolean extensible = concept.getExtensible();
    return extensible != null ? (extensible ? "Yes" : "No") : "";
  }

  public static String getTerminology(String terminologyPreferredName){
    if(terminologyPreferredName == null){
      return null;
    }
    String[] parts = terminologyPreferredName.split(" ");
    if(parts.length < 2){
      return terminologyPreferredName;
    }
    return terminologyPreferredName.split(" ")[1];
  }
}
