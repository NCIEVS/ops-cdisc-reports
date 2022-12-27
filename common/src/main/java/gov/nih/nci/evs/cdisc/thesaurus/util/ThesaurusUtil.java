package gov.nih.nci.evs.cdisc.thesaurus.util;

import gov.nih.nci.evs.cdisc.report.model.Synonym;
import gov.nih.nci.evs.cdisc.thesaurus.model.AlternativeDefinition;
import gov.nih.nci.evs.cdisc.thesaurus.model.Concept;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gov.nih.nci.evs.cdisc.report.utils.AssertUtils.assertRequired;

public class ThesaurusUtil {
  public static final String TERM_SOURCE_CDISC = "CDISC";
  public static final String TERM_SOURCE_NCI = "NCI";
  public static final String TERM_SOURCE_CDISC_GLOSSARY = "CDISC-GLOSS";

  public static final String TERM_GROUP_PREFERRED_TERM = "PT";
  public static final String TERM_GROUP_SYNONYM = "SY";
  public static final String TERM_GROUP_AB = "AB";

  public static boolean isGlossaryConcept(String label) {
    assertRequired(label, "label");
    return label.contains("Glossary");
  }

  public static List<Synonym> getSynonymsWithSourceCode(List<Synonym> synonyms) {
    if (synonyms != null) {
      return synonyms.stream()
          .filter(
              synonym ->
                  TERM_SOURCE_CDISC.equals(synonym.getTermSource())
                      && TERM_GROUP_PREFERRED_TERM.equals(synonym.getTermGroup())
                      && StringUtils.isNotBlank(synonym.getSourceCode()))
          .collect(Collectors.toList());
    }
    return null;
  }

  public static Synonym getSySynonym(List<Synonym> synonyms) {
    if (synonyms != null) {
      return synonyms.stream()
          .filter(
              synonym ->
                  TERM_SOURCE_CDISC.equals(synonym.getTermSource())
                      && TERM_GROUP_SYNONYM.equals(synonym.getTermSource()))
          .findFirst()
          .orElse(null);
    }
    return null;
  }

  public static void populateCodeListCodes(
      Concept rootConcept, Collection<Concept> allConcepts, List<Concept> allDescendantCodes) {
    List<Concept> childListCodes =
        allConcepts.stream()
            .filter(concept -> concept.getParents().contains(rootConcept.getCode()))
            .collect(Collectors.toList());
    allDescendantCodes.addAll(childListCodes);
  }

  public static void populateCodeInSubsets(
      Collection<Concept> allConcepts, List<Concept> codeListCodes) {
    List<Concept> subsetConcepts = new ArrayList<>();
    for (Concept concept : codeListCodes) {
      concept
          .getCodeInSubsets()
          .addAll(
              allConcepts.stream()
                  .filter(
                      subsetConcept -> subsetConcept.getSubsetCodes().contains(concept.getCode()))
                  .collect(Collectors.toList()));
      subsetConcepts.addAll(concept.getCodeInSubsets());
    }
    codeListCodes.addAll(subsetConcepts);
  }

  public static String decodeSpecialChar(String line) {
    line = line.replaceAll("&apos;", "'");
    line = line.replaceAll("&amp;", "&");
    line = line.replaceAll("&lt;", "<");
    line = line.replaceAll("&gt;", ">");
    line = line.replaceAll("&quot;", "\"");
    return line;
  }

  public static String getCdiscDefinition(Concept concept, boolean glossaryRootConcept) {
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

  public static Predicate<Synonym> getTermPredicate(String termGroup, String termSource) {
    return synonym ->
        termGroup.equals(synonym.getTermGroup()) && termSource.equals(synonym.getTermSource());
  }

  public static Predicate<Synonym> getTermPredicate(
      String termGroup, String termSource, String termName) {
    return synonym ->
        termGroup.equals(synonym.getTermGroup())
            && termSource.equals(synonym.getTermSource())
            && termName.equals(synonym.getTermName());
  }

  public static Predicate<Synonym> getSynonymSourceCodePredicate(
      String termGroup, String termSource, String sourceCode) {
    return synonym ->
        termGroup.equals(synonym.getTermGroup())
            && termSource.equals(synonym.getTermSource())
            && sourceCode.equals(synonym.getSourceCode());
  }

  public static String getExtensibleString(String code, Map<String, Concept> conceptMap) {
    Concept concept = conceptMap.get(code);
    if (concept != null) {
      Boolean extensible = concept.getExtensible();
      if (extensible != null) {
        return extensible ? "Yes" : "No";
      }
    }
    return "";
  }
}
