package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.model.Terminology;
import gov.nih.nci.evs.cdisc.report.model.XmlDataV2;
import gov.nih.nci.evs.cdisc.report.xml.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.glassfish.jaxb.core.marshaller.NoEscapeHandler;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static gov.nih.nci.evs.cdisc.report.TerminologyExcelReaderV2.getXmlDataList;
import static java.lang.String.format;

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
public class OdmConvertorV2 {
  private static final String ORIGINATOR =
      "CDISC XML Technologies Team (Terminology2ODM converter)";
  private static final String SOURCE_SYSTEM = "NCI Thesaurus";
  private static final String ODM_VERSION = "1.3.2";
  private static final String CONTROLLED_TERMINOLOGY_VERSION = "1.2.0";

  private final String asOfDateTime;
  private final String sourceSystemVersion;
  private final String fileOid;
  private final String context;

  private final String studyOid;
  private final String studyName;
  private final String studyDescription;
  private final String studyProtocolName;

  private final String metaDataVersionOid;
  private final String metaDataVersionName;
  private final String metaDataVersionDescription;

  private final String odmXmlFile;
  private final List<XmlDataV2> xmlDataList;

  public OdmConvertorV2(String strExcelFile, String odmXmlFile)
      throws IOException, InvalidFormatException {
    File excelFile = new File(strExcelFile);
    this.odmXmlFile = odmXmlFile;
    TerminologyExcelReaderV2 terminologyReader = new TerminologyExcelReaderV2();
    Terminology terminology = terminologyReader.getTerminology(excelFile);

    String model = terminology.getModel();
    String type = terminology.getType();
    String date = terminology.getDate();

    asOfDateTime = terminology.getDate() + "T00:00:00";
    sourceSystemVersion = terminology.getDate();
    fileOid = format("CDISC_CT.%s.%s", model, date);

    if (strExcelFile.contains("Glossary")
        || strExcelFile.contains("DDF")
        || strExcelFile.contains("Protocol")) {
      context = "Other";
    } else {
      context = "Submission";
    }

    studyOid = format("CDISC_CT.%s.%s", model, date);
    studyName = format("CDISC %s %s", model, type);
    studyDescription = format("CDISC %s %s, %s", model, type, date);
    studyProtocolName = format("CDISC %s %s", model, type);

    metaDataVersionOid = format("CDISC_CT_MetaDataVersion.%s.%s", model, date);
    metaDataVersionName = format("CDISC %s %s", model, type);
    metaDataVersionDescription = format("CDISC %s %s, %s", model, type, date);
    xmlDataList = getXmlDataList(strExcelFile, terminology.getSheetIndex());
  }

  public void generateOdmXml() throws IOException, DatatypeConfigurationException, JAXBException {
    JAXBContext jc = JAXBContext.newInstance("gov.nih.nci.evs.cdisc.report.xml");
    Marshaller marshaller = jc.createMarshaller();
    marshaller.setProperty("org.glassfish.jaxb.marshaller.CharacterEscapeHandler", NoEscapeHandler.theInstance);
    marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

    ODM odm = createODM();
    ODMcomplexTypeDefinitionStudy study = createStudy();
    ODMcomplexTypeDefinitionMetaDataVersion metaDataVersion =
        new ODMcomplexTypeDefinitionMetaDataVersion();
    metaDataVersion.setOID(metaDataVersionOid);
    metaDataVersion.setDescription(metaDataVersionDescription);
    metaDataVersion.setName(metaDataVersionName);
    ODMcomplexTypeDefinitionCodeList codeList = null;
    for (XmlDataV2 xmlData : xmlDataList) {
      if (StringUtils.isBlank(xmlData.getCodeListCode())) {
        codeList = createCodeList(xmlData);
        metaDataVersion.getCodeList().add(codeList);
      } else {
        ODMcomplexTypeDefinitionEnumeratedItem enumeratedItem = createEnumeratedItem(xmlData);
        codeList.getEnumeratedItem().add(enumeratedItem);
      }
    }
    study.getMetaDataVersion().add(metaDataVersion);
    odm.getStudy().add(study);
    try (Writer writer = new PrintWriter(odmXmlFile, StandardCharsets.UTF_8)) {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      marshaller.marshal(odm, writer);
    }
  }

  private ODM createODM() throws DatatypeConfigurationException {
    ODM odm = new ODM();
    odm.setFileType(FileType.SNAPSHOT);
    odm.setFileOID(fileOid);
    odm.setGranularity(Granularity.METADATA);
    odm.setCreationDateTime(getXmlGregorianCalendar(new Date()));
    odm.setAsOfDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(asOfDateTime));
    odm.setODMVersion(ODM_VERSION);
    odm.setOriginator(ORIGINATOR);
    odm.setSourceSystem(SOURCE_SYSTEM);
    odm.setSourceSystemVersion(sourceSystemVersion);
    odm.setContext(ODMContext.fromValue(context));
    odm.setControlledTerminologyVersion(CONTROLLED_TERMINOLOGY_VERSION);
    return odm;
  }

  private ODMcomplexTypeDefinitionStudy createStudy() {
    ODMcomplexTypeDefinitionStudy study = new ODMcomplexTypeDefinitionStudy();
    ODMcomplexTypeDefinitionGlobalVariables globalVariables =
        new ODMcomplexTypeDefinitionGlobalVariables();
    ODMcomplexTypeDefinitionProtocolName ctProtocolName =
        new ODMcomplexTypeDefinitionProtocolName();
    ODMcomplexTypeDefinitionStudyDescription ctStudyDescription =
        new ODMcomplexTypeDefinitionStudyDescription();
    ODMcomplexTypeDefinitionStudyName ctStudyName = new ODMcomplexTypeDefinitionStudyName();
    study.setOID(studyOid);
    ctProtocolName.setValue(studyProtocolName);
    ctStudyDescription.setValue(studyDescription);
    ctStudyName.setValue(studyName);
    globalVariables.setProtocolName(ctProtocolName);
    globalVariables.setStudyDescription(ctStudyDescription);
    globalVariables.setStudyName(ctStudyName);
    study.setGlobalVariables(globalVariables);
    return study;
  }

  private ODMcomplexTypeDefinitionCodeList createCodeList(XmlDataV2 xmlData) {
    ODMcomplexTypeDefinitionCodeList codeList = new ODMcomplexTypeDefinitionCodeList();
    codeList.setOID(
        format("CL.%s.%s", xmlData.getCode(), xmlEscapeText(xmlData.getSubmissionValue())));
    codeList.setName(xmlEscapeText(xmlData.getCodeListName()));
    codeList.setDataType(CLDataType.TEXT);
    codeList.setExtCodeID(xmlData.getCode());
    if (StringUtils.isNotBlank(xmlData.getCodeListExtensible())) {
      codeList.setCodeListExtensible(YesOrNo.fromValue(xmlData.getCodeListExtensible()));
    }
    ODMcomplexTypeDefinitionDescription clDescription = new ODMcomplexTypeDefinitionDescription();
    ODMcomplexTypeDefinitionTranslatedText clTranslatedText =
        new ODMcomplexTypeDefinitionTranslatedText();
    clTranslatedText.setValue(xmlEscapeText(xmlData.getCdiscDefinition()));
    clTranslatedText.setLang("en");
    clDescription.getTranslatedText().add(clTranslatedText);
    codeList.setDescription(clDescription);
    addCodeListElementExtension("CDISCSubmissionValue", xmlData.getSubmissionValue(), codeList);
    if (StringUtils.isNotBlank(xmlData.getSynonyms())) {
      String[] synonyms = getSynonyms(xmlData);
      for (String synonym : synonyms) {
        addCodeListElementExtension("CDISCSynonym", synonym, codeList);
      }
    }
    addCodeListElementExtension("PreferredTerm", xmlData.getNciPreferredTerm(), codeList);

    return codeList;
  }

  private ODMcomplexTypeDefinitionEnumeratedItem createEnumeratedItem(XmlDataV2 xmlData) {
    ODMcomplexTypeDefinitionEnumeratedItem enumeratedItem =
        new ODMcomplexTypeDefinitionEnumeratedItem();
    enumeratedItem.setCodedValue(xmlEscapeText(xmlData.getSubmissionValue()));
    enumeratedItem.setExtCodeID(xmlData.getCode());
    if (StringUtils.isNotBlank(xmlData.getSynonyms())) {
      String[] synonyms = getSynonyms(xmlData);
      for (String synonym : synonyms) {
        addEnumeratedItemElementExtension("CDISCSynonym", synonym, enumeratedItem);
      }
    }
    addEnumeratedItemElementExtension(
        "CDISCDefinition", xmlData.getCdiscDefinition(), enumeratedItem);
    addEnumeratedItemElementExtension(
        "PreferredTerm", xmlData.getNciPreferredTerm(), enumeratedItem);
    return enumeratedItem;
  }

  private void addEnumeratedItemElementExtension(
      String localPart, String value, ODMcomplexTypeDefinitionEnumeratedItem enumeratedItem) {
    JAXBElement<String> element =
        new JAXBElement(
            new QName("http://ncicb.nci.nih.gov/xml/odm/EVS/CDISC", localPart),
            String.class,
            xmlEscapeText(value));
    enumeratedItem.getEnumeratedItemElementExtension().add(element);
  }

  private void addCodeListElementExtension(
      String localPart, String value, ODMcomplexTypeDefinitionCodeList codeList) {
    JAXBElement<String> element =
        new JAXBElement(
            new QName("http://ncicb.nci.nih.gov/xml/odm/EVS/CDISC", localPart),
            String.class,
            xmlEscapeText(value));
    codeList.getCodeListElementExtension().add(element);
  }

  private XMLGregorianCalendar getXmlGregorianCalendar(Date date)
          throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(date);
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
  }

  private String xmlEscapeText(String t) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < t.length(); i++) {
      char c = t.charAt(i);
      switch (c) {
        case '<':
          sb.append("&lt;");
          break;
        case '>':
          sb.append("&gt;");
          break;
        case '\"':
          sb.append("&quot;");
          break;
        case '&':
          sb.append("&amp;");
          break;
        case '\'':
          sb.append("&apos;");
          break;
        default:
          if (c > 0x7e) {
            sb.append("&#" + ((int) c) + ";");
          } else {
            sb.append(c);
          }
      }
    }
    return sb.toString();
  }

  private String[] getSynonyms(XmlDataV2 xmlData){
    return StringUtils.stripAll(xmlData.getSynonyms().split(";"));
  }

  public static void main(String[] args)
          throws IOException, InvalidFormatException, DatatypeConfigurationException, JAXBException {
    if (args.length < 1) {
      log.error("Wrong number of parameters. Expected {}. Got {}", 1, args.length);
      log.error(
          "Usage: OdmConvertorV2 <path to excel report> <path for output odm file - optional>");
      log.error(
          "Example: OdmConvertorV2 \"~/temp/reports/SDTM Terminology.xls\" \"~/temp/reports/SDTM Terminology.odm.xml\"");
      System.exit(1);
    }
    String excelFile = args[0];
    String odmXmlFile;

    if (Files.exists(Path.of(excelFile))) {
      if (args.length == 1) {
        String excelFileName = FilenameUtils.getName(excelFile);
        odmXmlFile = excelFileName + ".odm.xml";
      } else {
        odmXmlFile = args[1];
      }
      OdmConvertorV2 convertor = new OdmConvertorV2(excelFile, odmXmlFile);
      convertor.generateOdmXml();
    } else {
      log.error("File {} does not exist", excelFile);
      System.exit(1);
    }
  }
}
