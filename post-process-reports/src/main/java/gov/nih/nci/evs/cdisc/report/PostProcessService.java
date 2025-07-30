package gov.nih.nci.evs.cdisc.report;

import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.getOutputPath;
import static gov.nih.nci.evs.cdisc.report.utils.ReportUtils.log;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.DeserializationFeature;
import com.amazonaws.lambda.thirdparty.com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.cdisc.report.model.ReportDetail;
import gov.nih.nci.evs.cdisc.report.model.ReportSummary;
import gov.nih.nci.evs.cdisc.report.utils.AssertUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;

public class PostProcessService {
  static final String ARCHIVE_DIRECTORY = "Archive";

  /**
   * When run through step functions, a parallel node will produce output as a list. Since we have
   * multiple and nested parallel nodes, we get a nested list. This function merges those lists
   * together and consolidates all the reports that were produced by the step function
   *
   * @param input required, representation of the JSON response produced by step function as raw
   *     lists and maps
   * @return Consolidated summary with all the reports that were created in the individual steps
   */
  public ReportSummary getReportSummary(List input) {
    AssertUtils.assertRequired(input, "input");
    List<ReportSummary> reportSummaries = new ArrayList<>();
    populateReportSummaries(input, reportSummaries);
    return mergeReportSummaries(reportSummaries);
  }

  /**
   * Some of the reports are expected to have the publication date as part of the file name and
   * copied to the "Archive" folder
   *
   * @param reportSummary contains all generated files
   */
  public void archiveFiles(ReportSummary reportSummary) {
    AssertUtils.assertRequired(reportSummary, "reportSummary");
    AssertUtils.assertRequired(reportSummary.getReportDetails(), "reportDetails");
    AssertUtils.assertRequired(reportSummary.getPublicationDate(), "publicationDate");
    String publicationDate = reportSummary.getPublicationDate();
    for (ReportDetail reportDetail : reportSummary.getReportDetails()) {
      Map<ReportEnum, String> reportMap = reportDetail.getReports();
      ReportEnum.ARCHIVE_REPORTS.stream()
          .forEach(reportEnum -> copyToArchive(reportEnum, reportMap, publicationDate));
    }
  }

  private void copyToArchive(
      ReportEnum reportEnum, Map<ReportEnum, String> reportMap, String publicationDate) {
    String reportFile = reportMap.get(reportEnum);
    if (reportFile == null) {
      log.info("No report found for report {}. Nothing to archive", reportEnum);
      return;
    }
    Path archiveFilePath = getArchiveFilePath(reportFile, publicationDate);
    try {
      Files.copy(Path.of(reportFile), archiveFilePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format(
              "Error occurred during copy to archive. ReportFile:%s. ArchiveFile:%s",
              reportFile, archiveFilePath),
          e);
    }
  }

  private Path getArchiveFilePath(String reportFilePath, String publicationDate) {
    File reportFile = new File(reportFilePath);
    Path archiveFolderPath = getOutputPath(reportFile.getParentFile().toPath(), ARCHIVE_DIRECTORY);
    String reportFileName = reportFile.getName();
    String archiveFileName = getArchiveFileName(reportFileName, publicationDate);
    return archiveFolderPath.resolve(archiveFileName);
  }

  static String getArchiveFileName(String reportFileName, String publicationDate) {
    return new StringBuilder(getBaseName(reportFileName))
        .append(" ")
        .append(publicationDate)
        .append(FilenameUtils.EXTENSION_SEPARATOR)
        .append(getExtension(reportFileName))
        .toString();
  }
  /**
   * Converts the raw list and maps into a list of summary objects that were created within each
   * parallel node
   *
   * @param input required, representation of the JSON response produced by step function as raw *
   *     lists and maps
   * @param summaries list to accumulate all the summary objects that are discovered by recursing
   *     through nested structure
   */
  private void populateReportSummaries(List input, List<ReportSummary> summaries) {
    for (Object node : input) {
      if (node instanceof List) {
        populateReportSummaries((List) node, summaries);
      } else {
        Map<String, ?> jsonObject = ((Map) node);
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final ReportSummary reportSummary = mapper.convertValue(jsonObject, ReportSummary.class);
        summaries.add(reportSummary);
      }
    }
  }

  /**
   * Merge all the report summaries to produce a single report summary with all the reports that
   * were produced during a run
   *
   * @param reportSummaries list to accumulate all the summary objects that are discovered by
   *     recursing through nested structure
   * @return consolidated summary
   */
  private ReportSummary mergeReportSummaries(List<ReportSummary> reportSummaries) {
    if (reportSummaries.isEmpty()) {
      return null;
    }
    ReportSummary reportSummary = reportSummaries.get(0);
    String publicationDate = reportSummary.getPublicationDate();
    List<String> deliveryEmailAddresses = reportSummary.getDeliveryEmailAddresses();
    // group all reports by code
    Map<String, List<ReportDetail>> reportsByCode =
        reportSummaries.stream()
                .filter(rs -> rs.getReportDetails() != null)
            .flatMap(rs -> rs.getReportDetails().stream())
            .collect(Collectors.groupingBy(ReportDetail::getCode));
    List<ReportDetail> reportDetails =
        reportsByCode.values().stream().reduce(new ArrayList<>(), this::accumulateList);
    return ReportSummary.builder()
        .publicationDate(publicationDate)
        .deliveryEmailAddresses(deliveryEmailAddresses)
        .reportDetails(reportDetails)
        .build();
  }

  private ReportDetail accumulateReportDetail(ReportDetail finalRd, ReportDetail currentRd) {
    // merge the reports. the value of the map will not change if the key is the same. So the
    // remapping function can return either value.
    currentRd
        .getReports()
        .forEach((key, value) -> finalRd.getReports().merge(key, value, (v1, v2) -> v1));
    return ReportDetail.builder()
        .code(currentRd.getCode())
        .label(currentRd.getLabel())
        .reports(finalRd.getReports())
        .build();
  }

  private List<ReportDetail> accumulateList(
      List<ReportDetail> finalList, List<ReportDetail> currentList) {
    finalList.add(currentList.stream().reduce(this::accumulateReportDetail).get());
    return finalList;
  }

  public static void main(String[] args) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    String json = new String(Files.readAllBytes(Paths.get("/Users/squareroot/temp/post_process.json")));
    List reports = objectMapper.readValue(json, List.class);
    PostProcessService postProcessService = new PostProcessService();
    ReportSummary reportSummary = postProcessService.getReportSummary(reports);
    System.out.println("ReportSummary: " + objectMapper.writeValueAsString(reportSummary));
  }
}
