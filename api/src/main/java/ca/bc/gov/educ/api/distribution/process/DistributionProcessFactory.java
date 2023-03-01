package ca.bc.gov.educ.api.distribution.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DistributionProcessFactory {
	
    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessFactory.class);

    @Autowired
    MergeProcess mergeProcess;

    @Autowired
    PostingSchoolReportProcess postingSchoolReportProcess;

    @Autowired
    CreateReprintProcess createReprintProcess;

    @Autowired
    CreateBlankCredentialProcess createBlankCredentialProcess;

    @Autowired
    PSIReportProcess pSIReportProcess;

	public DistributionProcess createProcess(DistributionProcessType processImplementation) {
		DistributionProcess pcs = null;
        switch(processImplementation.name()) {
            case "MER":
                logger.debug("\n************* MERGE PROCESS (MER) START  ************");
                pcs = mergeProcess;
                break;
            case "RPR":
                logger.debug("\n************* CREATE REPRINT PROCESS (RPR) START  ************");
                pcs = createReprintProcess;
                break;
            case "BCPR":
                logger.debug("\n************* CREATE BLANK CREDENTIAL PROCESS (BCPR) START  ************");
                pcs = createBlankCredentialProcess;
                break;
            case "MERYER":
                logger.debug("\n************* MERGE PROCESS (MERYER) START  ************");
                pcs = mergeProcess;
                break;
            case "PSR":
                logger.debug("\n************* POSTING SCHOOL REPORT PROCESS (PSR) START  ************");
                pcs = postingSchoolReportProcess;
                break;
            case "PSPR":
                logger.debug("\n************* PSI CREDENTIAL REPORT PROCESS (PSPR) START  ************");
                pcs = pSIReportProcess;
                break;
            default:
	        	break;
        }
        return pcs;
    }
}
