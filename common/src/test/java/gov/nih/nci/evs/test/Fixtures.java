package gov.nih.nci.evs.test;

import com.google.common.collect.Lists;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.model.ThesaurusRequest;

import java.util.List;

public class Fixtures {
    public static final String THESAURUS_FILE = "THESAURUS_FILE";
    public static final String CONCEPT_CODE_1 = "CONCEPT_CODE_1";
    public static final String CONCEPT_CODE_2 = "CONCEPT_CODE_2";
    public static final String DELIVERY_EMAIL_ADDRESS_1 = "DELIVERY_EMAIL_ADDRESS_1";
    public static final String PUBLICATION_DATE = "PUBLICATION_CODE";
    public static final List<String> CONCEPT_CODES =
            Lists.newArrayList(CONCEPT_CODE_1, CONCEPT_CODE_2);
    public static final List<String> DELIVERY_EMAIL_ADDRESSES =
            Lists.newArrayList(DELIVERY_EMAIL_ADDRESS_1);

    public static ThesaurusRequest getRequest() {
        return ThesaurusRequest.builder()
                .thesaurusOwlFile(THESAURUS_FILE)
                .conceptCodes(CONCEPT_CODES)
                .deliveryEmailAddresses(DELIVERY_EMAIL_ADDRESSES)
                .publicationDate(PUBLICATION_DATE)
                .build();
    }

    public static ReportSummary getReportSummary() {
        return ReportSummary.builder()
                .deliveryEmailAddresses(DELIVERY_EMAIL_ADDRESSES)
                .publicationDate(PUBLICATION_DATE)
                .build();
    }
}
