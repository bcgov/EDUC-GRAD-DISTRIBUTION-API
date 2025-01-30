package ca.bc.gov.educ.api.distribution.process;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DistributionProcessFactory {
	
    @Autowired
    MergeProcess mergeProcess;

    @Autowired
    YearEndMergeProcess yearEndMergeProcess;

    @Autowired
    PostingSchoolReportProcess postingSchoolReportProcess;

    @Autowired
    CreateReprintProcess createReprintProcess;

    @Autowired
    CreateBlankCredentialProcess createBlankCredentialProcess;

    @Autowired
    PSIReportProcess pSIReportProcess;

	public DistributionProcess createProcess(String processType) {
		DistributionProcess pcs = null;
        DistributionProcessType pt = DistributionProcessType.valueOf(processType);
        log.debug(String.format("Process Type: %s", pt));

        switch(pt) {
            case MER:
                log.debug("\n************* MERGE PROCESS (MER) START  ************");
                pcs = mergeProcess;
                break;
            case RPR:
                log.debug("\n************* CREATE REPRINT PROCESS (RPR) START  ************");
                pcs = createReprintProcess;
                break;
            case BCPR:
                log.debug("\n************* CREATE BLANK CREDENTIAL PROCESS (BCPR) START  ************");
                pcs = createBlankCredentialProcess;
                break;
            case MERYER:
                log.debug("\n************* MERGE PROCESS (MERYER) START  ************");
                pcs = yearEndMergeProcess;
                break;
            case MERSUPP:
                log.debug("\n************* MERGE PROCESS (MERSUPP) START  ************");
                pcs = mergeProcess;
                break;
            case PSR:
                log.debug("\n************* POSTING SCHOOL REPORT PROCESS (PSR) START  ************");
                pcs = postingSchoolReportProcess;
                break;
            case PSPR:
                log.debug("\n************* PSI CREDENTIAL REPORT PROCESS (PSPR) START  ************");
                pcs = pSIReportProcess;
                break;
            default:
	        	break;
        }
        return pcs;
    }
}
