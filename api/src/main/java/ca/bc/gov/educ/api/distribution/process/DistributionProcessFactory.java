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
                pcs = createBlankCredentialProcess;
                break;
            default:
	        	break;
        }
        return pcs;
    }
}
