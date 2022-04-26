package gov.nih.nci.evs.cdisc.report;
@FunctionalInterface
public interface CDISCReport {
    ReportResponse run(ReportContext context);
}
