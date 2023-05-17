package gov.nih.nci.evs.cdisc.report;

import com.google.common.collect.ImmutableList;
import gov.nih.nci.evs.cdisc.report.model.TextReport;
import gov.nih.nci.evs.cdisc.report.xml.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jaxb.core.marshaller.NoEscapeHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
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
 */

/**
 * @author EVS Team
 * @version 1.0
 *     <p>Modification history: Initial implementation kim.ong@nih.gov
 */
@Slf4j
public class RDFGeneratorV2 {
  private static final String STRING_DATATYPE_NAMESPACE = "http://www.w3.org/2001/XMLSchema#string";
  private final List<String> ONTOLOGY_IMPORTS =
      ImmutableList.of(
          "http://rdf.cdisc.org/mms",
          "http://rdf.cdisc.org/ct/schema",
          "http://purl.org/dc/elements/1.1/",
          "http://purl.org/dc/terms/",
          "http://www.w3.org/2004/02/skos/core");
  private final Set<String> subsetCodes = new HashSet<>();

  public void generate(String textFile, String owlFile) throws IOException, JAXBException {
    String terminology = getTerminologyAbbreviation(textFile);
    JAXBContext jc = JAXBContext.newInstance("gov.nih.nci.evs.cdisc.report.xml");
    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty(
        "org.glassfish.jaxb.marshaller.CharacterEscapeHandler", NoEscapeHandler.theInstance);
    marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    Map<String, List<TextReport>> textReportMap = convertToTextReportMap(textFile);
    RDF rdf = new RDF();
    Ontology ontology = createOntology();
    rdf.setOntology(ontology);
    rdf.getPermissibleValue().addAll(createPermissibleValues(textReportMap));
    try (Writer writer = new PrintWriter(owlFile, StandardCharsets.UTF_8)) {
      marshaller.marshal(rdf, writer);
    }
    /**
     * Some ugly hacks here. The default namespace is dynamically created based on the terminology.
     * So it cannot be specified in the package-info class. The alternate way to handle it is with
     * namespace prefix mapper. However, that was producing the default prefixes produced by the XML
     * vendor and the prefixes that we specify. This results in multiple prefixes for the same URL.
     * So doing this hack of using the package-info class to produce a templated namespace and then
     * replacing that value later on.
     */
    String content = IOUtils.toString(new FileInputStream(owlFile), StandardCharsets.UTF_8);
    content = content.replaceAll("\\{terminology\\}", terminology);
    /**
     * The xnl:base element is required by the RDF root element. However, I was unable to figure out
     * how to produce this through JAXB marshaller. So this hack is to produce a namespace called
     * base and do a string replace after the fact. Since the URL for default namespace and xml:base
     * is the same, the marshaller was not producing the default namespace. In order to work around
     * that, we use different template names despite having the same values to replace
     */
    content = content.replaceAll("xmlns:base", "xml:base");
    content = content.replaceAll("\\{base-terminology\\}", terminology);
    IOUtils.write(content, new FileOutputStream(owlFile), StandardCharsets.UTF_8);
  }

  private Map<String, List<TextReport>> convertToTextReportMap(String datafile) throws IOException {
    Map<String, List<TextReport>> reportMap = new HashMap<>();
    List<String> lines = IOUtils.readLines(new FileInputStream(datafile), Charset.defaultCharset());
    // Remove header
    lines.remove(0);
    for (String line : lines) {
      TextReport textReport = convertToTextReport(line);
      List<TextReport> textReports = reportMap.get(textReport.getCode());
      if (textReports == null) {
        textReports = new ArrayList<>();
      }
      textReports.add(textReport);
      reportMap.put(textReport.getCode(), textReports);
    }
    return reportMap;
  }

  private TextReport convertToTextReport(String line) {
    String[] tokens = line.split("\t");
    return new TextReport(
        tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[5], tokens[6], tokens[7]);
  }

  private List<PermissibleValue> createPermissibleValues(Map<String, List<TextReport>> reportMap) {
    List<PermissibleValue> permissibleValues = new ArrayList<>();
    for (Map.Entry<String, List<TextReport>> reportEntry : reportMap.entrySet()) {
      List<TextReport> reports = reportEntry.getValue();
      for (TextReport report : reports) {
        if (StringUtils.isNotBlank(report.getCodeListCode())) {
          TextReport codeListTextReport =
              reportMap.get(report.getCodeListCode()) != null
                  ? reportMap.get(report.getCodeListCode()).stream().findFirst().orElse(null)
                  : null;
          PermissibleValue permissibleValue = new PermissibleValue();
          String id = String.format("%s.%s", report.getCodeListCode(), report.getCode());
          PermissibleValue.InValueDomain valueDomain;
          if (subsetCodes.add(report.getCodeListCode())) {
            valueDomain = createInValueDomain(codeListTextReport, false);
          } else {
            valueDomain = createInValueDomain(codeListTextReport, true);
          }
          CdiscDefinition cdiscDefinition = new CdiscDefinition();
          cdiscDefinition.setDatatype(STRING_DATATYPE_NAMESPACE);
          cdiscDefinition.setValue(encode(report.getCdiscDefinition()));

          NciPreferredTerm nciPreferredTerm = new NciPreferredTerm();
          nciPreferredTerm.setDatatype(STRING_DATATYPE_NAMESPACE);
          nciPreferredTerm.setValue(encode(report.getNciPreferredTerm()));
          if (StringUtils.isNotBlank(report.getCdiscSynonyms())) {
            CdiscSynonyms synonyms = new CdiscSynonyms();
            synonyms.setDatatype(STRING_DATATYPE_NAMESPACE);
            synonyms.setValue(encode(report.getCdiscSynonyms()));
            permissibleValue.setCdiscSynonyms(synonyms);
          }
          CdiscSubmissionValue submissionValue = new CdiscSubmissionValue();
          submissionValue.setDatatype(STRING_DATATYPE_NAMESPACE);
          submissionValue.setValue(encode(report.getCdiscSubmissionValue()));

          NciCode nciCode = new NciCode();
          nciCode.setDatatype(STRING_DATATYPE_NAMESPACE);
          nciCode.setValue(report.getCode());

          permissibleValue.setID(id);
          permissibleValue.setInValueDomain(valueDomain);
          permissibleValue.setCdiscDefinition(cdiscDefinition);
          permissibleValue.setNciPreferredTerm(nciPreferredTerm);
          permissibleValue.setNciCode(nciCode);
          permissibleValue.setCdiscSubmissionValue(submissionValue);
          permissibleValues.add(permissibleValue);
        }
      }
    }
    return permissibleValues;
  }

  private PermissibleValue.InValueDomain createInValueDomain(TextReport report, boolean exists) {
    if (report == null) {
      return null;
    }
    PermissibleValue.InValueDomain inValueDomain;
    if (exists) {
      inValueDomain = new PermissibleValue.InValueDomain();
      inValueDomain.setResource("#" + report.getCode());
    } else {
      inValueDomain = new PermissibleValue.InValueDomain();
      PermissibleValue.InValueDomain.EnumeratedValueDomain enumeratedValueDomain =
          new PermissibleValue.InValueDomain.EnumeratedValueDomain();

      CdiscDefinition cdiscDefinition = new CdiscDefinition();
      cdiscDefinition.setDatatype(STRING_DATATYPE_NAMESPACE);
      cdiscDefinition.setValue(encode(report.getCdiscDefinition()));

      NciPreferredTerm nciPreferredTerm = new NciPreferredTerm();
      nciPreferredTerm.setDatatype(STRING_DATATYPE_NAMESPACE);
      nciPreferredTerm.setValue(encode(report.getNciPreferredTerm()));

      NciCode nciCode = new NciCode();
      nciCode.setDatatype(STRING_DATATYPE_NAMESPACE);
      nciCode.setValue(report.getCode());

      CdiscSynonyms synonyms = new CdiscSynonyms();
      synonyms.setDatatype(STRING_DATATYPE_NAMESPACE);
      synonyms.setValue(encode(report.getCdiscSynonyms()));

      CdiscSubmissionValue submissionValue = new CdiscSubmissionValue();
      submissionValue.setDatatype(STRING_DATATYPE_NAMESPACE);
      submissionValue.setValue(encode(report.getCdiscSubmissionValue()));

      CodelistName codelistName = new CodelistName();
      codelistName.setDatatype(STRING_DATATYPE_NAMESPACE);
      codelistName.setValue(encode(report.getCodeListName()));

      if (StringUtils.isNotBlank(report.getCodeListExtensible())) {
        IsExtensibleCodelist isExtensibleCodelist = new IsExtensibleCodelist();
        isExtensibleCodelist.setDatatype("http://www.w3.org/2001/XMLSchema#boolean");
        isExtensibleCodelist.setValue("Yes".equals(report.getCodeListExtensible()));
        enumeratedValueDomain.setIsExtensibleCodelist(isExtensibleCodelist);
      }

      enumeratedValueDomain.setID(report.getCode());
      enumeratedValueDomain.setCdiscDefinition(cdiscDefinition);
      enumeratedValueDomain.setNciPreferredTerm(nciPreferredTerm);
      enumeratedValueDomain.setNciCode(nciCode);
      enumeratedValueDomain.setCdiscSynonyms(synonyms);
      enumeratedValueDomain.setCdiscSubmissionValue(submissionValue);
      enumeratedValueDomain.setCodelistName(codelistName);
      inValueDomain.setEnumeratedValueDomain(enumeratedValueDomain);
    }
    return inValueDomain;
  }

  private <T> T createAndSetStringValue(
      Supplier<T> supplier,
      BiConsumer<T, String> dataTypeFunction,
      BiConsumer<T, String> valueFunction,
      String value) {
    T object = supplier.get();
    dataTypeFunction.accept(object, STRING_DATATYPE_NAMESPACE);
    valueFunction.accept(object, value);
    return object;
  }

  private String getTerminologyAbbreviation(String datafile) {
    String baseName = FilenameUtils.getBaseName(datafile);
    String[] tokens = baseName.split(" ");
    String abbreviation = tokens[0];
    if (tokens.length == 1) {
      tokens = baseName.split("_");
      abbreviation = tokens[0];
    }
    return abbreviation.toLowerCase();
  }

  private Ontology createOntology() {
    Ontology ontology = new Ontology();
    ontology.setAbout("");
    Ontology.VersionInfo versionInfo = new Ontology.VersionInfo();
    versionInfo.setDatatype(STRING_DATATYPE_NAMESPACE);
    versionInfo.setValue("Created with RDFGenerator");
    // This is adapted from RDFGenerator. That class had a particular sequence of elements with
    // Ontology. In order to match that sequence, we are splitting up the imports elements
    ontology
        .getImportsOrVersionInfo()
        .addAll(
            ONTOLOGY_IMPORTS.subList(0, 2).stream()
                .map(this::createImports)
                .collect(Collectors.toList()));
    ontology.getImportsOrVersionInfo().add(versionInfo);
    ontology
        .getImportsOrVersionInfo()
        .addAll(
            ONTOLOGY_IMPORTS.subList(2, ONTOLOGY_IMPORTS.size()).stream()
                .map(this::createImports)
                .collect(Collectors.toList()));
    return ontology;
  }

  private Ontology.Imports createImports(String resource) {
    Ontology.Imports objImport = new Ontology.Imports();
    objImport.setResource(resource);
    return objImport;
  }

  /**
   * Escapes characters for rendering in XML. We should be using {@link
   * org.apache.commons.text.StringEscapeUtils#escapeXml11(String)}. But the existing code ({@link
   * RDFGenerator}) was only escaping the characters that are in this method. In order to not
   * disrupt consumers, we are porting over this method from there
   *
   * @return escaped string
   */
  private static String encode(String term) {
    if (term == null) return null;
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < term.length(); i++) {
      char c = term.charAt(i);
      if (c == '<') {
        buf.append("&lt;");
      } else if (c == '>') {
        buf.append("&gt;");
      } else if (c == '&') {
        buf.append("&amp;");
      } else {
        buf.append(c);
      }
    }
    return buf.toString();
  }

  public static void main(String[] args) throws IOException, JAXBException {
    if (args.length < 1) {
      log.error("Wrong number of parameters. Expected {}. Got {}", 1, args.length);
      log.error(
          "Usage: RDFGeneratorV2 <path to concept text file> <path for output owl file - optional>");
      log.error(
          "Example: RDFGeneratorV2 \"~/temp/reports/SDTM Terminology.odm.xml\" \"~/temp/reports/SDTM Terminology.owl\"");
      System.exit(1);
    }
    String textFile = args[0];
    String owlFile;
    if (Files.exists(Path.of(textFile))) {
      if (args.length == 1) {
        String textFileName = FilenameUtils.getName(textFile);
        owlFile = textFileName + ".owl";
      } else {
        owlFile = args[1];
      }
      RDFGeneratorV2 rdfGenerator = new RDFGeneratorV2();
      rdfGenerator.generate(textFile, owlFile);
    } else {
      log.error("File {} does not exist", textFile);
      System.exit(1);
    }
  }
}
