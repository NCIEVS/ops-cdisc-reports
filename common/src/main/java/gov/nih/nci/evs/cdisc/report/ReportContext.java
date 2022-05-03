package gov.nih.nci.evs.cdisc.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ReportContext {
    private File inputFile;
    private List<String> rootCodes;
    private Path outputDirectory;
    private String publicationDate;
}
