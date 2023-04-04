package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.IOUtils;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.internal.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data
@Component
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PSIReportProcess extends BaseProcess{
	
	private static Logger logger = LoggerFactory.getLogger(PSIReportProcess.class);
	private static final String ADDRESS_LABEL_PSI = "ADDRESS_LABEL_PSI";

	@Override
	public ProcessorData fire(ProcessorData processorData) {
		long sTime = System.currentTimeMillis();
		logger.debug("************* TIME START   ************ {}",sTime);
		DistributionResponse disRes = new DistributionResponse();
		Map<String,DistributionPrintRequest> mDist = processorData.getMapDistribution();
		Long bId = processorData.getBatchId();
		int numOfPdfs = 0;
		int cnter=0;
		List<School> schoolsForLabels = new ArrayList<>();
		for (Map.Entry<String, DistributionPrintRequest> entry : mDist.entrySet()) {
			cnter++;
			int currentSlipCount = 0;
			String psiCode = entry.getKey();
			DistributionPrintRequest obj = entry.getValue();
			Psi psiDetails = psiService.getPsiDetails(psiCode,restUtils.getAccessToken());
			if(psiDetails != null) {
				logger.debug("*** PSI Details Acquired {}", psiDetails.getPsiName());
				ReportRequest packSlipReq = reportService.preparePackingSlipDataPSI(psiDetails, processorData.getBatchId());
				Pair<Integer,Integer> pV = processTranscriptPrintRequest(obj,currentSlipCount,packSlipReq,processorData,psiCode,numOfPdfs);
				numOfPdfs = pV.getRight();
				logger.debug("PDFs Merged {}", psiDetails.getPsiName());
				processSchoolsForLabels(schoolsForLabels, psiDetails);
				if (cnter % 50 == 0) {
					restUtils.fetchAccessToken(processorData);
				}
				logger.debug("PSI {}/{}",cnter,mDist.size());
			}
		}
		restUtils.fetchAccessToken(processorData);
		logger.debug("***** Create and Store school labels reports *****");
		int numberOfCreatedSchoolLabelReports = createSchoolLabelsReport(schoolsForLabels, processorData.getAccessToken(), ADDRESS_LABEL_PSI );
		logger.debug("***** Number of created school labels reports {} *****", numberOfCreatedSchoolLabelReports);
		logger.debug("***** Distribute school labels reports *****");
		int numberOfProcessedSchoolLabelsReports = processDistrictSchoolDistribution(processorData, ADDRESS_LABEL_PSI, null, null);
		logger.debug("***** Number of distributed school labels reports {} *****", numberOfProcessedSchoolLabelsReports);
		numOfPdfs += numberOfProcessedSchoolLabelsReports;
		postingProcess(bId,processorData,numOfPdfs);
		long eTime = System.currentTimeMillis();
		long difference = (eTime - sTime)/1000;
		logger.debug("************* TIME Taken  ************ {} secs",difference);
		disRes.setMergeProcessResponse("Merge Successful and File Uploaded");
		processorData.setDistributionResponse(disRes);
		return processorData;
	}

	private Pair<Integer, Integer> processTranscriptPrintRequest(DistributionPrintRequest obj, int currentSlipCount, ReportRequest packSlipReq, ProcessorData processorData, String psiCode, int numOfPdfs) {
		if (obj.getPsiCredentialPrintRequest() != null) {
			PsiCredentialPrintRequest psiCredentialPrintRequest = obj.getPsiCredentialPrintRequest();
			List<PsiCredentialDistribution> scdList = psiCredentialPrintRequest.getPsiList();
			List<InputStream> locations = new ArrayList<>();
			currentSlipCount++;
			setExtraDataForPackingSlip(packSlipReq, "YED4", obj.getTotal(), scdList.size(), 1, "Transcript", psiCredentialPrintRequest.getBatchId());
			try {
				locations.add(reportService.getPackingSlip(packSlipReq, restUtils.getAccessToken()).getInputStream());
				logger.debug("*** Packing Slip Added");
				//Grad2-1931 : processing students transcripts, merging them and placing in tmp location for transmission mode FTP to generate CSV files- mchintha
				if (processorData.getTransmissionMode().equalsIgnoreCase("FTP")) {
					String csv = processStudentsForCSVs(scdList,processorData);
					mergeDocumentsCSVs(processorData, psiCode, "02", "/GRAD_INT", "_RESULTS", csv);
				}
				else {
					processStudentsForPDFs(scdList,locations,processorData);
					mergeDocumentsPDFs(processorData, psiCode, "02", "/EDGRAD.T.", "YED4", locations);
				}
				numOfPdfs++;
				logger.debug("*** Transcript Documents Merged");
			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return Pair.of(currentSlipCount,numOfPdfs);
	}



	private void processStudentsForPDFs(List<PsiCredentialDistribution> scdList, List<InputStream> locations, ProcessorData processorData) throws IOException {
		int currentTranscript = 0;
		int failedToAdd = 0;

		for (PsiCredentialDistribution scd : scdList) {
			InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptUsingStudentID(), scd.getStudentID())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
			if (transcriptPdf != null) {
				locations.add(transcriptPdf.getInputStream());
				currentTranscript++;
				logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
				}
			else {
				failedToAdd++;
				logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
				}
		}
	}
	//Grad2-1931 : Writes students transcripts data on CSV and formatting them - mchintha
	private String processStudentsForCSVs(List<PsiCredentialDistribution> scdList, ProcessorData processorData) throws IOException {
		int currentTranscript = 0;
		int failedToAdd = 0;
		String[] studentInfo = null;
		String[] schoolInfo = null;
		String[] ExaminableCoursesAndAssessmentsInfo = null;
		String[] nonExaminableCoursesInfo = null;
		int[] rowAallColumnsWidths = new int[19];
		int[] rowBallColumnsWidths = new int[7];
		int[] rowCallColumnsWidths = new int[15];
		int[] rowDallColumnsWidths = new int[16];
		int[][] allRowsColumnsWidths;
		String csv = null;
		CsvMapper csvMapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withLineSeparator("\r\n");
		List<String[]> studentTranscriptdata = new ArrayList<>();
		List<String[]> updatedStudentTranscriptdataList = new ArrayList<>();
		for (PsiCredentialDistribution scd : scdList) {

				ReportData transcriptCsv = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptCSVData(), scd.getPen())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(ReportData.class).block();
				if(transcriptCsv !=null) {
					Student studentDetails = transcriptCsv.getStudent();
					School schoolDetails = transcriptCsv.getSchool();
					List<TranscriptResult> courseDetails = (transcriptCsv.getTranscript() != null ? transcriptCsv.getTranscript().getResults() : null);

                    //Writes the A's row's data on CSV
					if(studentDetails != null) {
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat(educDistributionApiConstants.DATE_FORMAT);
						studentInfo = new String[]{
								scd.getPen(), "A", studentDetails.getLastName(), studentDetails.getFirstName(), studentDetails.getMiddleName(),
								simpleDateFormat.format(studentDetails.getBirthdate()), studentDetails.getGender(), studentDetails.getCitizenship(),
								studentDetails.getGrade(), "", studentDetails.getLocalId(), "", "", "", "", "", "", "", "", studentDetails.getGradProgram()};

						rowAallColumnsWidths = IntStream.of(10,1,25,25,25,8,1,1,2,8,12,2,1,4,6,1,1,15,18,4).toArray();
						for (int i = 0; i < studentInfo.length; i++) {
							String formattedString = String.format("%-" + rowAallColumnsWidths[i] + "s", (studentInfo[i] != null ? studentInfo[i] : ""));
							studentInfo[i] = formattedString;
						}
						studentTranscriptdata.add(studentInfo);
					}
					//Writes the B's row's data on CSV
					if(schoolDetails != null) {
						schoolInfo = new String[]{
								scd.getPen(), "B", schoolDetails.getAddress().getStreetLine1(), schoolDetails.getAddress().getStreetLine2(), schoolDetails.getAddress().getCity(),
								schoolDetails.getAddress().getCode(), schoolDetails.getAddress().getCountry(), ""};

						rowBallColumnsWidths = IntStream.of(10,1,40,40,30,2,2,7).toArray();
						for (int i = 0; i < schoolInfo.length; i++) {
							String formattedString = String.format("%-" + rowBallColumnsWidths[i] + "s", (schoolInfo[i] != null ? schoolInfo[i] : ""));
							schoolInfo[i] = formattedString;
						}
						studentTranscriptdata.add(schoolInfo);
					}
					//Writes the C's data on CSV
					if(courseDetails != null) {
						for (TranscriptResult course : courseDetails) {
							//C rows writes Examinable Courses and Assessments
							if (course.getCourse().getType().equals("1") || course.getCourse().getType().equals("3")) {
								ExaminableCoursesAndAssessmentsInfo = new String[]{
										scd.getPen(), "C", course.getCourse().getCode(), course.getCourse().getLevel(), course.getCourse().getSessionDate(),
										course.getMark().getInterimLetterGrade(), "", course.getMark().getSchoolPercent(), "", course.getMark().getExamPercent(),
										course.getMark().getFinalPercent(), course.getMark().getFinalLetterGrade(), course.getMark().getInterimPercent(), "", "", ""};

								rowCallColumnsWidths = IntStream.of(10,1,5,3,6,2,1,3,1,3,3,2,3,2,1,1).toArray();
								for (int i = 0; i < ExaminableCoursesAndAssessmentsInfo.length; i++) {
									String formattedString = String.format("%-" + rowCallColumnsWidths[i] + "s", (ExaminableCoursesAndAssessmentsInfo[i] != null ? ExaminableCoursesAndAssessmentsInfo[i] : ""));
									ExaminableCoursesAndAssessmentsInfo[i] = formattedString;
								}
								studentTranscriptdata.add(ExaminableCoursesAndAssessmentsInfo);
							}
						}
					}
					//Writes D's rows data on CSV
					if(courseDetails != null) {
						for (TranscriptResult course : courseDetails) {
							//D rows writes only Non-Examinable Courses
							if(course.getCourse().getType().equals("2")) {
								nonExaminableCoursesInfo = new String[]{
										scd.getPen(), "D", course.getCourse().getCode(), course.getCourse().getLevel(), course.getCourse().getSessionDate(),
										course.getMark().getInterimLetterGrade(), "", course.getMark().getSchoolPercent(), "", course.getMark().getExamPercent(),
										course.getMark().getFinalPercent(), course.getMark().getFinalLetterGrade(), course.getMark().getInterimPercent(), "", "", "",""};

								rowDallColumnsWidths = IntStream.of(10,1,5,3,6,2,2,3,3,2,5,3,40,1,1,1,1).toArray();
								for (int i = 0; i < nonExaminableCoursesInfo.length; i++) {
									String formattedString = String.format("%-" + rowDallColumnsWidths[i] + "s", (nonExaminableCoursesInfo[i] != null ? nonExaminableCoursesInfo[i] : ""));
									nonExaminableCoursesInfo[i] = formattedString;
								}
								studentTranscriptdata.add(nonExaminableCoursesInfo);
							}
						}
					}


					//retrieving each string from string arrays of list and getting rid of double quotes after performing the null check
					updatedStudentTranscriptdataList = studentTranscriptdata.stream()
							.map(arr -> Arrays.stream(arr)
									.map(s -> {
										if (s != null && s.startsWith("\"") && s.endsWith("\"")) {
											return s.substring(1, s.length() - 1);
											} else {
											return s;
											}
										}).toArray(String[]::new))
							.collect(Collectors.toList());
					csv = csvMapper.writer(schema).writeValueAsString(updatedStudentTranscriptdataList);
					currentTranscript++;
					logger.debug("*** Added csv {}/{} Current student {}", currentTranscript, scdList.size(), scd.getPen());
				}
				else {
					failedToAdd++;
					logger.debug("*** Failed to Add {} Current student {}", failedToAdd, scd.getPen());
				}

			}
		return csv;
	}

}
