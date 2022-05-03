package gov.nih.nci.evs.cdisc.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public interface CDISCReport {
  public static final Logger log = LoggerFactory.getLogger(CDISCReport.class);

  default List<ReportResponse> run(ReportContext context) {
    List<ReportResponse> responses = new ArrayList<>();
    if (context.getRootCodes() != null && !context.getRootCodes().isEmpty()) {
      initialize(context);
      for (String root : context.getRootCodes()) {
        ReportResponse response = run(root, context);
        responses.add(response);
      }
    } else {
      log.warn("No root codes in request");
    }
    return responses;
  }

  default void initialize(ReportContext context) {}

  ReportResponse run(String root, ReportContext context);
}
