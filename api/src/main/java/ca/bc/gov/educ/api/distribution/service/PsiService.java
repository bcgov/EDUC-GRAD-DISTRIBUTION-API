package ca.bc.gov.educ.api.distribution.service;

import ca.bc.gov.educ.api.distribution.model.dto.Psi;
import ca.bc.gov.educ.api.distribution.model.dto.ReportData;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PsiService {

    RestService restService;
	EducDistributionApiConstants educDistributionApiConstants;
	RestUtils restUtils;

	@Autowired
	public PsiService(RestService restService, EducDistributionApiConstants educDistributionApiConstants, RestUtils restUtils) {
		this.restService = restService;
		this.educDistributionApiConstants = educDistributionApiConstants;
		this.restUtils = restUtils;
	}

	public Psi getPsiDetails(String psiCode) {
		return restService.executeGet(
				educDistributionApiConstants.getPsiByPsiCode(),
				Psi.class,
				psiCode
		);
	}
    //Grad2-1931 Retrieving Student Transcript - mchintha
	public ReportData getReportData(String pen) {
		return restService.executeGet(
				educDistributionApiConstants.getTranscriptCSVData(),
				ReportData.class,
				pen
		);
	}
}
