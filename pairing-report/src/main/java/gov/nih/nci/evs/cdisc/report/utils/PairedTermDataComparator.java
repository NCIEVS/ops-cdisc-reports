package gov.nih.nci.evs.cdisc.report.utils;

import gov.nih.nci.evs.cdisc.report.model.PairedTermData;

import java.util.Comparator;

public class PairedTermDataComparator implements Comparator<PairedTermData> {
    @Override
    public int compare(PairedTermData o1, PairedTermData o2) {
        int sourceCodeComparison = o1.getSourceCode().compareTo(o2.getSourceCode());
        if(sourceCodeComparison == 0){
            return o1.getMemberCode().compareTo(o2.getMemberCode());
        }
        return sourceCodeComparison;
    }
}
