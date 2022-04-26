package gov.nih.nci.evs.cdisc.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.nio.file.Path;

@Data
@Builder
@AllArgsConstructor
public class ReportContext {
    private File inputFile;
    private String rootCode;
    private Path outputDirectory;
}
