package gov.nih.nci.evs.cdisc.report;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.commons.io.FilenameUtils;

import java.io.FileInputStream;
import java.io.IOException;

public class PdfHtmlHeaderAndFooter {
    public static void main(String[] args) throws IOException {
        PdfHtmlHeaderAndFooter.manipulatePdf(args[0], args[1]);
    }

    public static void manipulatePdf(String htmlSource, String pdfDest) throws IOException {
        PdfWriter writer = new PdfWriter(pdfDest);
        PdfDocument pdfDocument = new PdfDocument(writer);
        Footer footerHandler = new Footer();

        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, footerHandler);

        // Base URI is required to resolve the path to source files
        ConverterProperties converterProperties = new ConverterProperties().setBaseUri(FilenameUtils.getPath(htmlSource));
        HtmlConverter.convertToDocument(new FileInputStream(htmlSource), pdfDocument, converterProperties);

        // Write the total number of pages to the placeholder
        footerHandler.writeTotal(pdfDocument);
        pdfDocument.close();
    }
}
