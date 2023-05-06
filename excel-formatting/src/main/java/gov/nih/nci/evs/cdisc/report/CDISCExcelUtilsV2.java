package gov.nih.nci.evs.cdisc.report;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.util.Iterator;

@Slf4j
public class CDISCExcelUtilsV2 {
  private static final String DEFAULT_AUTHOR = "NCI-EVS";

  private final HSSFWorkbook workbook;
  private final String publicationDate;

  public CDISCExcelUtilsV2(String filename, String publicationDate) throws IOException {
    this.publicationDate = publicationDate;
    try (FileInputStream fis = new FileInputStream(filename)) {
      this.workbook = new HSSFWorkbook(fis);
    }
  }

  public CDISCExcelUtilsV2(HSSFWorkbook workbook, String publicationDate) {
    this.publicationDate = publicationDate;
    this.workbook = workbook;
  }

  public int getNumberOfRows(Sheet sheet) {
    int rowTotal = sheet.getLastRowNum();
    if ((rowTotal > 0) || (sheet.getPhysicalNumberOfRows() > 0)) {
      rowTotal++;
    }
    return rowTotal;
  }

  public void setAutoFilter(Sheet sheet, char lastColumnChar) {
    int numRows = getNumberOfRows(sheet);
    sheet.setAutoFilter(CellRangeAddress.valueOf("A1:" + lastColumnChar + numRows));
  }

  private static String getCellData(Cell cell) {
    switch (cell.getCellTypeEnum()) {
      case BOOLEAN:
        return "" + cell.getBooleanCellValue();
      case STRING:
        return cell.getRichStringCellValue().getString();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return ("" + cell.getDateCellValue());
        } else {
          return ("" + cell.getNumericCellValue());
        }
      case FORMULA:
        return (cell.getCellFormula());
      default:
        return "";
    }
  }

  public HSSFWorkbook format(String xlsFile) throws IOException, InvalidFormatException {
    log.info("Formatting excel sheet {}", xlsFile);
    int sheetIndex = 0;
    FileOutputStream formattedExcelReportOut;
    try (InputStream excelReportIn = new FileInputStream(xlsFile)) {
      HSSFWorkbook formattedWorkbook = (HSSFWorkbook) WorkbookFactory.create(excelReportIn);
      Sheet sheet = formattedWorkbook.getSheetAt(sheetIndex);

      setColumnWidths(sheet);
      setSheetName(formattedWorkbook, xlsFile, sheetIndex);

      Font font = getFont(formattedWorkbook);

      Iterator<Row> rows = sheet.rowIterator();
      int row_num = 0;
      while (rows.hasNext()) {
        Row row = rows.next();
        row.setHeight((short) -1);
        if (row_num == 0) {
          row.setHeight((short) 950);
        }
        Cell cell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        String codeListCode = getCellData(cell);

        boolean codeListRow = StringUtils.isBlank(codeListCode);

        for (int i = 0; i < row.getLastCellNum(); i++) {
          cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
          CellStyle cellStyle = cell.getCellStyle();
          cellStyle.setWrapText(true); // Wrapping text
          if (row_num == 0) {
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
          } else {
            cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
          }
          if (codeListRow) {
            cellStyle.setFillBackgroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            cellStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
            cellStyle.setFont(font);
          }
          cellStyle.setBorderTop(BorderStyle.THIN);
          cellStyle.setBorderBottom(BorderStyle.THIN);
          cellStyle.setBorderLeft(BorderStyle.THIN);
          cellStyle.setBorderRight(BorderStyle.THIN);
          cell.setCellStyle(cellStyle);
        }
        row_num++;
      }

      setAutoFilter(sheet, 'H');
      log.info("Completed formatting. Writing formatted excel file {}", xlsFile);
      formattedExcelReportOut = new FileOutputStream(xlsFile);
      formattedWorkbook.write(formattedExcelReportOut);
      return formattedWorkbook;
    }
  }

  /*
  c.	Set the column widths (there are specific parameters to follow for each column)
  i.	A-B are width of 8
  ii.	C is a width of 12
  iii.	D-F is a width of 35
  iv.	G is a width of 64
  v.	H is a width of 35
  */

  private void setColumnWidths(Sheet sheet) {
    sheet.setColumnWidth(0, 8 * 256); // A
    sheet.setColumnWidth(1, 8 * 256); // B
    sheet.setColumnWidth(2, 12 * 256); // C
    sheet.setColumnWidth(3, 35 * 256); // D
    sheet.setColumnWidth(4, 35 * 256); // E
    sheet.setColumnWidth(5, 35 * 256); // F
    sheet.setColumnWidth(6, 64 * 256); // G
    sheet.setColumnWidth(7, 35 * 256); // H
  }

  private void setSheetName(Workbook workbook, String xlsfile, int sheetIndex) {
    String sheetName = FilenameUtils.getBaseName(xlsfile);
    if (sheetName.contains("Glossary")) {
      sheetName = sheetName.replace("Glossary", "Glossary Terminology");
    }
    sheetName = sheetName.replace("CDISC", "").replace("_", " ");
    sheetName = sheetName + " " + publicationDate;
    sheetName = sheetName.replace("Define-XML", "Def-XML");
    log.info("Setting sheet name to {}", sheetName);
    workbook.setSheetName(sheetIndex, sheetName.trim());
  }

  private Font getFont(Workbook workbook) {
    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setBold(false);
    font.setItalic(false);
    return font;
  }

  public void setMetadata(
      String title, String author, String subject, String keywords, String comments) {
    log.info("Setting metadata");
    this.workbook.createInformationProperties();
    SummaryInformation summaryInfo = this.workbook.getSummaryInformation();
    if (title != null) {
      summaryInfo.setTitle(title);
    }
    if (author != null) {
      summaryInfo.setAuthor(author);
    } else {
      summaryInfo.setAuthor(DEFAULT_AUTHOR);
    }
    if (subject != null) {
      summaryInfo.setSubject(subject);
    }
    if (keywords != null) {
      summaryInfo.setKeywords(keywords);
    }
    if (comments != null) {
      summaryInfo.setComments(comments);
    }
  }

  public void saveWorkbook(String filename) throws IOException {
    log.info("Saving excel workbook {}", filename);
    try (OutputStream os = new FileOutputStream(filename)) {
      this.workbook.write(os);
    }
  }

  public static void main(String[] args) throws IOException, InvalidFormatException {
    if (args.length < 2) {
      log.error("Wrong number of parameters. Expected {}. Got {}", 2, args.length);
      log.error("Usage: CDISCExcelUtilsV2 <path to Excel report> <publication date>");
      log.error("Example: CDISCExcelUtilsV2 /tmp/Thesaurus-230320-23.03c_fixed.xls 2023-01-01");
      System.exit(1);
    }

    String xlsFile = args[0];
    String publicationDate = args[1];

    HSSFWorkbook formattedWorkbook =
        new CDISCExcelUtilsV2(xlsFile, publicationDate).format(xlsFile);
    String title = FilenameUtils.getBaseName(xlsFile);
    String author = DEFAULT_AUTHOR;
    CDISCExcelUtilsV2 cdiscExcelUtils = new CDISCExcelUtilsV2(formattedWorkbook, publicationDate);
    cdiscExcelUtils.setMetadata(title, author, title, title, null);
    cdiscExcelUtils.saveWorkbook(xlsFile);
  }
}
