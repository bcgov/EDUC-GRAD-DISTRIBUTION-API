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
import java.util.List;
import java.util.Map;

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
				processStudents(scdList,locations,processorData);
				mergeDocuments(processorData,psiCode,"02","/EDGRAD.T.","YED4",locations);
				numOfPdfs++;
				logger.debug("*** Transcript Documents Merged");
			} catch (IOException e) {
				logger.debug(EXCEPTION,e.getLocalizedMessage());
			}
		}
		return Pair.of(currentSlipCount,numOfPdfs);
	}



	private void processStudents(List<PsiCredentialDistribution> scdList, List<InputStream> locations, ProcessorData processorData) throws IOException {
		int currentTranscript = 0;
		int failedToAdd = 0;
		String[] studentInfo = null;
		String[] schoolInfo = null;
		String[] coursesInfo = null;
		String[] assessmentsInfo = null;
		File file = new File("C:\\Users\\mchintha\\IdeaProjects\\EDUC-GRAD-DISTRIBUTION-API\\api\\src\\main\\resources\\myCSV.csv");
		CsvMapper csvMapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withLineSeparator("\r\n");
		List<String[]> studentTranscriptdata = new ArrayList<>();
		for (PsiCredentialDistribution scd : scdList) {

            //Grad2-1931

			if(processorData.getTransmissionMode().equalsIgnoreCase("FTP")) {
				ReportData transcriptCsv = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptCSVData(), scd.getPen())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(ReportData.class).block();
				if(transcriptCsv !=null) {
				Student studentDetails = transcriptCsv.getStudent();
				School schoolDetails = transcriptCsv.getSchool();
				List<TranscriptResult> courseDetails = transcriptCsv.getTranscript().getResults();


				if(studentDetails != null) {
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(educDistributionApiConstants.DATE_FORMAT);
					studentInfo = new String[]{
							scd.getPen(), "A", studentDetails.getLastName(), studentDetails.getFirstName(), studentDetails.getMiddleName(),simpleDateFormat.format(studentDetails.getBirthdate()),
							studentDetails.getGender(), studentDetails.getCitizenship(), studentDetails.getGrade(), "", studentDetails.getLocalId(), "", "", "", "", "", "", "", "", studentDetails.getGradProgram()};
					studentTranscriptdata.add(studentInfo);
					}
				if(schoolDetails != null) {
					schoolInfo = new String[]{
							scd.getPen(), "B", schoolDetails.getAddress().getStreetLine1(), schoolDetails.getAddress().getStreetLine2(), schoolDetails.getAddress().getCity(),
							schoolDetails.getAddress().getCode(), schoolDetails.getAddress().getCountry(), ""};
					studentTranscriptdata.add(schoolInfo);
					}
				if(courseDetails != null) {
					for (TranscriptResult course : courseDetails) {
						if(!course.getCourse().getType().equals("3")) {
							coursesInfo = new String[]{
									scd.getPen(), "C", course.getCourse().getCode(), course.getCourse().getLevel(), course.getCourse().getSessionDate(),
									course.getMark().getInterimLetterGrade(), "", course.getMark().getSchoolPercent(), "", course.getMark().getExamPercent(),
									course.getMark().getFinalPercent(), course.getMark().getFinalLetterGrade(), course.getMark().getInterimPercent(), "", "", ""};
							studentTranscriptdata.add(coursesInfo);
							}
						}
					}
				if(courseDetails != null) {
					for (TranscriptResult assessment : courseDetails) {
						if(assessment.getCourse().getType().equals("3")) {
							assessmentsInfo = new String[]{
									scd.getPen(), "D", assessment.getCourse().getCode(), assessment.getCourse().getLevel(), assessment.getCourse().getSessionDate(),
									assessment.getMark().getInterimLetterGrade(), "", assessment.getMark().getSchoolPercent(), "", assessment.getMark().getExamPercent(),
									assessment.getMark().getFinalPercent(), assessment.getMark().getFinalLetterGrade(), assessment.getMark().getInterimPercent(), "", "", ""};
							studentTranscriptdata.add(assessmentsInfo);
							}
						}
					}

				csvMapper.writer(schema).writeValueAsString(studentTranscriptdata);
				currentTranscript++;
				logger.debug("*** Added csv {}/{} Current student {}", currentTranscript, scdList.size(), scd.getPen());
				}
				else {
					failedToAdd++;
					logger.debug("*** Failed to Add {} Current student {}", failedToAdd, scd.getPen());
				}

			}
			else {
				InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptUsingStudentID(), scd.getStudentID())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
				if (transcriptPdf != null) {
					locations.add(transcriptPdf.getInputStream());
					currentTranscript++;
					logger.debug("*** Added PDFs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getStudentID());
				} else {
					failedToAdd++;
					logger.debug("*** Failed to Add PDFs {} Current student {}", failedToAdd, scd.getStudentID());
				}
			}
		}
	}

}
