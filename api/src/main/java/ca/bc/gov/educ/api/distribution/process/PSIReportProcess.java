package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import com.fasterxml.jackson.core.JsonGenerator;
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
		for (PsiCredentialDistribution scd : scdList) {
			InputStreamResource transcriptPdf = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptUsingStudentID(), scd.getStudentID())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(InputStreamResource.class).block();
            //Grad2-1931
			if(processorData.getTransmissionMode().equalsIgnoreCase("FTP")) {
				ReportData transciptCsv = webClient.get().uri(String.format(educDistributionApiConstants.getTranscriptCSVData(), scd.getPen())).headers(h -> h.setBearerAuth(restUtils.fetchAccessToken())).retrieve().bodyToMono(ReportData.class).block();

				if(transciptCsv !=null) {
				Student studentDetails = transciptCsv.getStudent();
				School schoolDetails = transciptCsv.getSchool();
				List<TranscriptResult> courses = transciptCsv.getTranscript().getResults();

				String[] studentInfo = null;
				String[] schoolinfo = null;
				String[] coursesInfo = null;

				File file = new File("C:\\Users\\mchintha\\IdeaProjects\\EDUC-GRAD-DISTRIBUTION-API\\api\\src\\main\\resources\\myCSV.csv");
				CsvMapper csvMapper = new CsvMapper();
				CsvSchema schema = CsvSchema.emptySchema()
						.withLineSeparator("\r\n");// instead of \n

				List<String[]> studentTranscriptdata = new ArrayList<>();
				if(studentDetails != null) {
					studentInfo = new String[]{
							scd.getPen(), "A", studentDetails.getLastName(), studentDetails.getFirstName(), studentDetails.getMiddleName(), studentDetails.getBirthdate().toString(),
							studentDetails.getGender(), studentDetails.getCitizenship(), studentDetails.getGrade(), "", studentDetails.getLocalId(), "", "", "", "", "", "", "", "", studentDetails.getGradProgram()};
				}
				if(schoolDetails != null) {
					schoolinfo = new String[]{
							scd.getPen(), "B", schoolDetails.getAddress().getStreetLine1(), schoolDetails.getAddress().getStreetLine2(), schoolDetails.getAddress().getCity(),
							schoolDetails.getAddress().getCode(), schoolDetails.getAddress().getCountry(), ""};
				}
				if(courses != null) {
				for (TranscriptResult course : courses) {
					coursesInfo = new String[]{
							scd.getPen(), "C", course.getCourse().getCode(), course.getCourse().getLevel(), course.getCourse().getSessionDate(),
							course.getMark().getInterimLetterGrade(), "", course.getMark().getSchoolPercent(), "", course.getMark().getExamPercent(),
							course.getMark().getFinalPercent(), course.getMark().getFinalLetterGrade(), course.getMark().getInterimPercent(), "", "", ""};
				}
				}
				studentTranscriptdata.add(studentInfo);
				studentTranscriptdata.add(schoolinfo);
				studentTranscriptdata.add(coursesInfo);

				csvMapper.writer(schema).writeValue(file, studentTranscriptdata);
				currentTranscript++;
				logger.debug("*** Added csvs {}/{} Current student {}", currentTranscript, scdList.size(), scd.getPen());
				}
				else {
					failedToAdd++;
					logger.debug("*** Failed to Add {} Current student {}", failedToAdd, scd.getPen());
				}

			}
			else {

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
