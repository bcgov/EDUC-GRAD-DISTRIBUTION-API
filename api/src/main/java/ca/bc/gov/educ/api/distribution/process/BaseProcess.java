package ca.bc.gov.educ.api.distribution.process;

import ca.bc.gov.educ.api.distribution.model.dto.CommonSchool;
import ca.bc.gov.educ.api.distribution.model.dto.DistributionPrintRequest;
import ca.bc.gov.educ.api.distribution.model.dto.ExceptionMessage;
import ca.bc.gov.educ.api.distribution.model.dto.ProcessorData;
import ca.bc.gov.educ.api.distribution.service.AccessTokenService;
import ca.bc.gov.educ.api.distribution.service.ReportService;
import ca.bc.gov.educ.api.distribution.service.SchoolService;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import ca.bc.gov.educ.api.distribution.util.GradValidation;
import ca.bc.gov.educ.api.distribution.util.SFTPUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class BaseProcess implements DistributionProcess{

    protected static final String LOC = "/tmp/";
    protected static final String DEL = "/";
    protected static final String EXCEPTION = "Error {} ";

    @Autowired
    GradValidation validation;

    @Autowired
    WebClient webClient;

    @Autowired
    EducDistributionApiConstants educDistributionApiConstants;

    @Autowired
    AccessTokenService accessTokenService;

    @Autowired
    SchoolService schoolService;

    @Autowired
    ReportService reportService;

    @Autowired
    SFTPUtils sftpUtils;

    protected CommonSchool getBaseSchoolDetails(DistributionPrintRequest obj, String mincode, ProcessorData processorData, ExceptionMessage exception) {
        if(obj.getProperName() != null)
            return schoolService.getDetailsForPackingSlip(obj.getProperName());
        else
            return schoolService.getSchoolDetails(mincode,processorData.getAccessToken(),exception);
    }

}
