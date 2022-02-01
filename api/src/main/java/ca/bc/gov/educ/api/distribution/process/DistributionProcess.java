package ca.bc.gov.educ.api.distribution.process;

import org.springframework.stereotype.Component;

@Component
public interface DistributionProcess {

	ProcessorData fire();

    public void setInputData(ProcessorData inputData);
}
