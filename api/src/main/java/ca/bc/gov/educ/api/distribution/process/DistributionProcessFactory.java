package ca.bc.gov.educ.api.distribution.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DistributionProcessFactory {
	
    private static final Logger logger = LoggerFactory.getLogger(DistributionProcessFactory.class);

    @Autowired
    TranscriptDataFileProcess transcriptDataFileProcess;

    @Autowired
    CertificateDataFileProcess certificateDataFileProcess;


    @Autowired
    MergeProcess mergeProcess;

	public DistributionProcess createProcess(DistributionProcessType processImplementation) {
		DistributionProcess pcs = null;
        switch(processImplementation.name()) {
            case "TPP":
                logger.info("\n************* TRANSCRIPT PRINT PROCESS (TPP) START  ************");
                pcs = transcriptDataFileProcess;
                break;
            case "CPP":
                logger.info("\n************* CERTIFICATE PRINT PROCESS (CPP) START  ************");
                pcs = certificateDataFileProcess;
                break;
            case "MER":
                logger.info("\n************* MERGE PROCESS (MER) START  ************");
                pcs = mergeProcess;
                break;

            default:
	        	break;
        }
        return pcs;
    }
}
