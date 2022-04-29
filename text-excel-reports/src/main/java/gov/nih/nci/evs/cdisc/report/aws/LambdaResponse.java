package gov.nih.nci.evs.cdisc.report.aws;

import gov.nih.nci.evs.cdisc.report.ReportResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LambdaResponse {
    private List<ReportResponse> textReports;
    private String publicationDate;
}
