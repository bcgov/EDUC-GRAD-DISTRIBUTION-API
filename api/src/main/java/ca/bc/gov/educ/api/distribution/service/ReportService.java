package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ReportService {

	@Autowired
    WebClient webClient;
	
	@Autowired
	EducDistributionApiConstants educDistributionApiConstants;
	
	public InputStreamResource getPackingSlip(ReportRequest packingSlipReq, String accessToken) {
		try
		{
			byte[] packingSlip = webClient.post().uri(educDistributionApiConstants.getPackingSlip()).headers(h -> h.setBearerAuth(accessToken)).body(BodyInserters.fromValue(packingSlipReq)).retrieve().bodyToMono(byte[].class).block();
			ByteArrayInputStream bis = new ByteArrayInputStream(packingSlip);
			return new InputStreamResource(bis);
		} catch (Exception e) {
			return null;
		}
	}

	public ReportRequest preparePackingSlipData(SchoolTrax schoolDetails,Long batchId) {
		ReportRequest req = new ReportRequest();

		School schObj = new School();
		Address addr = new Address();
		addr.setStreetLine1(schoolDetails.getAddress1());
		addr.setStreetLine2(schoolDetails.getAddress2());
		addr.setCity(schoolDetails.getCity());
		addr.setCode(schoolDetails.getPostal());
		addr.setCountry(schoolDetails.getCountryCode());
		addr.setRegion(schoolDetails.getProvCode());
		schObj.setAddress(addr);
		schObj.setDistno(schoolDetails.getMinCode().substring(0, 3));
		schObj.setName(schoolDetails.getSchoolName());
		schObj.setSchlno(schoolDetails.getMinCode());
		schObj.setMincode(schoolDetails.getMinCode());


		ReportOptions options = new ReportOptions();
		options.setReportFile("Packing Slip");
		options.setReportName("packingSlip");
		options.setConvertTo("pdf");
		options.setCacheReport(false);
		options.setOverwrite(false);

		ReportData data = new ReportData();
		PackingSlip pSlip = new PackingSlip();
		pSlip.setRecipient("ADMINISTRATION");
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

    public ReportRequest prepareSchoolDistributionReportData(SchoolDistributionRequest schoolDistributionRequest, Long batchId,SchoolTrax schoolDetails) {
		ReportRequest req = new ReportRequest();
		ReportData data = new ReportData();

		ReportOptions options = new ReportOptions();
		options.setReportFile("school distribution");
		options.setReportName("schoolDistribution");
		options.setConvertTo("pdf");
		options.setCacheReport(false);
		options.setOverwrite(false);

		List<StudentCredentialDistribution> schoolReportList = schoolDistributionRequest.getStudentList();
		List<Student> stdList = new ArrayList<>();
		School schObj = new School();
		schObj.setMincode(schoolDetails.getMinCode());
		schObj.setName(schoolDetails.getSchoolName());
		for(StudentCredentialDistribution sc:schoolReportList) {
			Student std = new Student();
			std.setFirstName(sc.getLegalFirstName());
			std.setLastName(sc.getLegalLastName());
			std.setMiddleName(sc.getLegalMiddleNames());
			Pen pen = new Pen();
			pen.setPen(sc.getPen());
			std.setPen(pen);
			std.setGradProgram(sc.getProgram());
			GraduationData gradData = new GraduationData();
			gradData.setGraduationDate(sc.getProgramCompletionDate() != null ? EducDistributionApiUtils.parsingTraxDate(sc.getProgramCompletionDate()):null);
			gradData.setHonorsFlag(sc.getHonoursStanding() != null && sc.getHonoursStanding().equalsIgnoreCase("Y"));
			std.setGraduationData(gradData);
			stdList.add(std);
		}
		schObj.setStudents(stdList);
		data.setSchool(schObj);
		data.setOrgCode(StringUtils.startsWith(data.getSchool().getMincode(), "098") ? "YU" : "BC");
		data.setReportNumber(data.getOrgCode()+"-"+batchId);
		req.setData(data);
		req.setOptions(options);

		return req;
    }
}
