package gov.nih.nci.evs.cdisc.report;

import com.google.common.collect.Sets;

import java.util.Set;

public enum ReportEnum {
  MAIN_TEXT,
  MAIN_EXCEL,
  CHANGES_TEXT,
  ODM_XML,
  MAIN_HTML,
  PDF_HTML,
  MAIN_PDF,
  MAIN_OWL,
  OWL_ZIP,
  PAIRING_EXCEL;

  public static final Set<ReportEnum> ARCHIVE_REPORTS =
      Sets.newHashSet(MAIN_HTML, ODM_XML, OWL_ZIP, PDF_HTML, MAIN_TEXT, MAIN_EXCEL);
}
