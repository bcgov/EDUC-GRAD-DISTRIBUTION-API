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

	public DistributionProcess createProcess(DistributionProcessType processImplementation) {
		DistributionProcess pcs = null;
        switch(processImplementation.name()) {
            case "MER":
                logger.info("\n************* MERGE PROCESS (MER) START  ************");
                pcs = mergeProcess;
                break;
            case "RPR":
                logger.info("\n************* CREATE REPRINT PROCESS (RPR) START  ************");
                pcs = createReprintProcess;
                break;
            case "BCPR":
                logger.info("\n************* CREATE BLANK CREDENTIAL PROCESS (BCPR) START  ************");
                pcs = createBlankCredentialProcess;
                break;
            case "MERYER":
                logger.info("\n************* MERGE PROCESS (MERYER) START  ************");
                pcs = mergeProcess;
                break;
            case "PSR":
                logger.info("\n************* POSTING SCHOOL REPORT PROCESS (PSR) START  ************");
                pcs = postingSchoolReportProcess;
                break;
            default:
	        	break;
        }
        return pcs;
    }
}
