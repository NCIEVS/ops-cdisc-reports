package gov.nih.nci.evs.cdisc.report;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.commons.io.FilenameUtils;

import java.io.FileInputStream;
import java.io.IOException;

public class HtmlToPdfConverter {

  /**
   * Converts HTML specifically generated to create PDFs to PDF.
   *
   * @param htmlSource file pathof HTML file
   * @param pdfDest output PDF destination
   * @throws IOException
   */
  public static void convert(String htmlSource, String pdfDest) throws IOException {
    PdfWriter writer = new PdfWriter(pdfDest);
    PdfDocument pdfDocument = new PdfDocument(writer);
    pdfDocument.setDefaultPageSize(PageSize.A3);
    FooterEventHandler footerHandler = new FooterEventHandler();

    pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

    // Base URI is required to resolve the path to source files
    ConverterProperties converterProperties =
        new ConverterProperties().setBaseUri(FilenameUtils.getPath(htmlSource));
    HtmlConverter.convertToDocument(
        new FileInputStream(htmlSource), pdfDocument, converterProperties);

    // Write the total number of pages to the placeholder
    footerHandler.writeTotal(pdfDocument);
    pdfDocument.close();
  }

  public static void main(String[] args) throws IOException {
    if (args.length >= 1) {
      String htmlSource = args[0];
      String pdfTarget = htmlSource.replace("-pdf.html", ".pdf");
      if (args.length == 2) {
        pdfTarget = args[1];
      }
      HtmlToPdfConverter.convert(htmlSource, pdfTarget);
    } else {
      System.out.println("Expecting path to pdf-html file");
    }
  }
}
