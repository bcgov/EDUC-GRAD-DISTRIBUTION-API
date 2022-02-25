package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.*;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.util.Date;

@Service
public class ReportService {

	@Autowired
    WebClient webClient;
	
	@Autowired
	EducDistributionApiConstants educDistributionApiConstants;
	
	public InputStreamResource getPackingSlip(ReportRequest packingSlipReq, String accessToken, ExceptionMessage exception) {
		try
		{
			byte[] packingSlip = webClient.post().uri(educDistributionApiConstants.getPackingSlip()).headers(h -> h.setBearerAuth(accessToken)).body(BodyInserters.fromValue(packingSlipReq)).retrieve().bodyToMono(byte[].class).block();
			ByteArrayInputStream bis = new ByteArrayInputStream(packingSlip);
			return new InputStreamResource(bis);
		} catch (Exception e) {
			exception.setExceptionName("GRAD-REPORT-API IS DOWN");
			exception.setExceptionDetails(e.getLocalizedMessage());
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
}
