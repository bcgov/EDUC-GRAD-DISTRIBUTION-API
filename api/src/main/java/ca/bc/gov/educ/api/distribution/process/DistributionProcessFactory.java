package ca.bc.gov.educ.api.distribution.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistributionProcessFactory {
	
    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessFactory.class);
    
	public DistributionProcess createProcess(DistributionProcessType processImplementation) {
		DistributionProcess pcs = null;
        switch(processImplementation.name()) {
	        default:
	        	break;
        }
        return pcs;
    }
}
