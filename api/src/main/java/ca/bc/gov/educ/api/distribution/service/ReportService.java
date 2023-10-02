package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import ca.bc.gov.educ.api.distribution.util.JsonTransformer;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ReportService {

	private static Logger logger = LoggerFactory.getLogger(ReportService.class);

    WebClient webClient;

	JsonTransformer jsonTransformer;

	RestUtils restUtils;

	EducDistributionApiConstants educDistributionApiConstants;

	final RestService restService;

	@Autowired
	public ReportService(WebClient webClient, RestUtils restUtils, EducDistributionApiConstants educDistributionApiConstants, RestService restService, JsonTransformer jsonTransformer) {
		this.webClient = webClient;
		this.restUtils = restUtils;
		this.educDistributionApiConstants = educDistributionApiConstants;
		this.restService = restService;
		this.jsonTransformer = jsonTransformer;
	}

	public InputStreamResource getPackingSlip(ReportRequest packingSlipReq) {
		logger.debug("Getting packing slip for order {}", packingSlipReq.getData().getPackingSlip().getOrderNumber());
		if(logger.isDebugEnabled()) {
			String packingSlipJson = jsonTransformer.marshall(packingSlipReq);
			logger.debug(packingSlipJson);
		}
		byte[] packingSlip = restService.executePost(educDistributionApiConstants.getPackingSlip(), byte[].class, packingSlipReq, "");
		ByteArrayInputStream bis = new ByteArrayInputStream(packingSlip);
		return new InputStreamResource(bis);
	}

	public ReportRequest preparePackingSlipData(StudentSearchRequest searchRequest, CommonSchool schoolDetails, Long batchId) {
		School schObj = new School();
		boolean useSchoolAddress = searchRequest == null || searchRequest.getAddress() == null;
		Address addr = useSchoolAddress ? new Address() : searchRequest.getAddress();
		if(useSchoolAddress) {
			addr.setStreetLine1(schoolDetails.getScAddressLine1());
			addr.setStreetLine2(schoolDetails.getScAddressLine2());
			addr.setCity(schoolDetails.getScCity());
			addr.setCode(schoolDetails.getScPostalCode());
			addr.setCountry(schoolDetails.getScCountryCode());
			addr.setRegion(schoolDetails.getScProvinceCode());
		}
		schObj.setAddress(addr);
		schObj.setDistno(schoolDetails.getDistNo());
		schObj.setName(schoolDetails.getSchoolName());
		schObj.setSchlno(schoolDetails.getSchlNo());
		schObj.setMincode(schoolDetails.getDistNo()+schoolDetails.getSchlNo());
		return  createReportRequest(batchId,schObj, searchRequest.getUser());
	}

	public ReportRequest preparePackingSlipDataPSI(Psi psiDetails,Long batchId) {
		School schObj = new School();
		Address addr = new Address();
		addr.setStreetLine1(psiDetails.getAddress1());
		addr.setStreetLine2(psiDetails.getAddress2());
		addr.setCity(psiDetails.getCity());
		addr.setCode(psiDetails.getPostal());
		addr.setCountry(psiDetails.getCountryCode());
		addr.setRegion(psiDetails.getCity());
		schObj.setAddress(addr);
		schObj.setDistno("000");
		schObj.setName(psiDetails.getPsiName());
		schObj.setSchlno(psiDetails.getPsiCode());
		schObj.setMincode(schObj.getDistno()+schObj.getSchlno());
		return createReportRequest(batchId,schObj, "ADMINISTRATION");
	}

	private ReportRequest createReportRequest(Long batchId, School schObj, String recipient) {
		ReportRequest req = new ReportRequest();
		ReportOptions options = new ReportOptions();
		options.setReportFile("Packing Slip");
		options.setReportName("packingSlip");
		options.setConvertTo("pdf");
		options.setCacheReport(false);
		options.setOverwrite(false);

		ReportData data = new ReportData();
		PackingSlip pSlip = new PackingSlip();
		pSlip.setRecipient(ObjectUtils.defaultIfNull(StringUtils.trimToNull(recipient), "ADMINISTRATION"));
		pSlip.setOrderNumber(batchId);
		pSlip.setOrderDate(EducDistributionApiUtils.formatIssueDateForReportJasper(EducDistributionApiUtils.getSimpleDateFormat(new Date())));
		pSlip.setOrderedBy("SILVER");
		PackingSlipOrderType orderType = new PackingSlipOrderType();
		PackingSlipType slipType = new PackingSlipType();
		PaperType ptype = new PaperType();
		slipType.setName("S");
		slipType.setPaperType(ptype);
		orderType.setPackingSlipType(slipType);
		pSlip.setOrderType(orderType);
		pSlip.setSchool(schObj);
		data.setPackingSlip(pSlip);
		req.setOptions(options);
		req.setData(data);
		return req;
	}

    public ReportRequest prepareSchoolDistributionReportData(SchoolDistributionRequest schoolDistributionRequest, Long batchId,CommonSchool schoolDetails) {
		ReportRequest req = new ReportRequest();
		ReportData data = new ReportData();

		List<StudentCredentialDistribution> schoolReportList = schoolDistributionRequest.getStudentList();
		List<Student> stdList = new ArrayList<>();
		School schObj = new School();
		schObj.setMincode(schoolDetails.getDistNo()+schoolDetails.getSchlNo());
		schObj.setName(schoolDetails.getSchoolName());
		for(StudentCredentialDistribution sc:schoolReportList) {
			Student std = new Student();
			std.setFirstName(sc.getLegalFirstName());
			std.setLastName(sc.getLegalLastName());
			std.setMiddleName(sc.getLegalMiddleNames());
			std.setCitizenship(sc.getStudentCitizenship());
			Pen pen = new Pen();
			pen.setPen(sc.getPen());
			pen.setEntityID("" + sc.getStudentID());
			std.setPen(pen);
			std.setGradProgram(sc.getProgram());
			GraduationData gradData = new GraduationData();
			gradData.setGraduationDate(sc.getProgramCompletionDate() != null ? EducDistributionApiUtils.asDate(sc.getProgramCompletionDate()) : null);
			gradData.setHonorsFlag(sc.getHonoursStanding() != null && sc.getHonoursStanding().equalsIgnoreCase("Y"));
			std.setGraduationData(gradData);

			std.setGraduationStatus(GraduationStatus.builder()
					.programCompletionDate(sc.getProgramCompletionDate())
					.honours(sc.getHonoursStanding())
					.programName(sc.getProgram())
					.studentGrade(sc.getStudentGrade())
					.schoolOfRecord(sc.getSchoolOfRecord())
					.build());

			stdList.add(std);
		}
		//No dups for school report
		List<Student> uniqueStudentList = new ArrayList<>(new LinkedHashSet<>(stdList));
		schObj.setStudents(uniqueStudentList);
		data.setSchool(schObj);
		data.setOrgCode(StringUtils.startsWith(data.getSchool().getMincode(), "098") ? "YU" : "BC");
		data.setReportNumber(data.getOrgCode()+"-"+batchId);
		req.setData(data);

		ReportOptions options = new ReportOptions();
		options.setConvertTo("pdf");
		options.setCacheReport(false);
		options.setOverwrite(false);
		options.setReportFile(String.format("%s School distribution.%s", schObj.getMincode(), options.getConvertTo()));
		options.setReportName("SchoolDistribution");

		req.setOptions(options);

		return req;
    }
}
