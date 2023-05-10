package gov.nih.nci.evs.cdisc.report;

import gov.nih.nci.evs.cdisc.report.model.Terminology;
import gov.nih.nci.evs.cdisc.report.model.XmlDataV2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
public class TerminologyExcelReaderV2 {

  private int findTerminologySheetIndex(HSSFWorkbook workbook) {
    int sheetCount = workbook.getNumberOfSheets();
    System.out.println("sheetCount: " + sheetCount);
    for (int i = 0; i < sheetCount; i++) {
      String sheetName = workbook.getSheetName(i);
      System.out.println("sheetName: " + sheetName);
      if (sheetName.contains("Terminology")) {
        return i;
      }
    }
    return -1;
  }

  public Terminology getTerminology(File file) throws IOException {
    HSSFSheet sheet;
    String sheetName;
    int sheetIndex;

    try (FileInputStream is = new FileInputStream(file);
        POIFSFileSystem fs = new POIFSFileSystem(is);
        HSSFWorkbook workbook = new HSSFWorkbook(fs)) {
      sheetIndex = findTerminologySheetIndex(workbook);
      if (sheetIndex == -1) {
        throw new RuntimeException(
            String.format("Unable to locate sheet of terminology in %s", file.getName()));
      }
      sheet = workbook.getSheetAt(sheetIndex);
      sheetName = sheet.getSheetName();
    }

    String[] parts = StringUtils.stripAll(sheetName.split(" "));
    if (parts.length != 3) {
      throw new RuntimeException(
          "Expected sheet name in form '<type> Terminology <date>' but found '"
              + sheetName
              + "' instead");
    }
    String terminologyModel = getTerminologyModel(parts);

    return new Terminology(
        terminologyModel, terminologyModel, "Controlled Terminology", parts[2], sheetIndex);
  }

  private String getTerminologyModel(String[] parts) {
    return parts[0].replace("Def-XML", "Define-XML");
  }

  public static List<XmlDataV2> getXmlDataList(String excelFile, int sheetNumber)
      throws IOException, InvalidFormatException {
    List<XmlDataV2> xmlDataList = new ArrayList<>();
    try (Workbook workbook = WorkbookFactory.create(new File(excelFile))) {
      Sheet sheet = workbook.getSheetAt(sheetNumber);
      Iterator<Row> rowIterator = sheet.rowIterator();
      while (rowIterator.hasNext()) {
        Row row = rowIterator.next();
        int lastCellNumber = row.getLastCellNum();
        XmlDataV2 xmlData = new XmlDataV2();

        for (int lcv = 0; lcv < lastCellNumber; lcv++) {
          Cell cell = row.getCell(lcv);
          String cellValue = "";
          if (cell != null) {
            cellValue = getCellValue(cell);
          }
          switch (lcv) {
            case 0:
              xmlData.setCode(cellValue);
            case 1:
              xmlData.setCodeListCode(cellValue);
            case 2:
              xmlData.setCodeListExtensible(cellValue);
            case 3:
              xmlData.setCodeListName(cellValue);
            case 4:
              xmlData.setSubmissionValue(cellValue);
            case 5:
              xmlData.setSynonyms(cellValue);
            case 6:
              xmlData.setCdiscDefinition(cellValue);
            case 7:
              xmlData.setNciPreferredTerm(cellValue);
          }
        }
        xmlDataList.add(xmlData);
      }
    }
    // Remove header
    xmlDataList.remove(0);
    return xmlDataList;
  }

  private static String getCellValue(Cell cell) {
    switch (cell.getCellTypeEnum()) {
      case BOOLEAN:
        System.out.print(cell.getBooleanCellValue());
        boolean bool = cell.getBooleanCellValue();
        return "" + bool;

      case STRING:
        return (cell.getRichStringCellValue().getString());

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
}
