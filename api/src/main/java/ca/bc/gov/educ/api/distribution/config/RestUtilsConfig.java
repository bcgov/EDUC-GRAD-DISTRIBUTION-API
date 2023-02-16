package ca.bc.gov.educ.api.distribution.config;

import ca.bc.gov.educ.api.distribution.model.dto.ResponseObjCache;
import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestUtilsConfig {

    EducDistributionApiConstants constants;

    @Autowired
    public RestUtilsConfig(EducDistributionApiConstants constants) {
        this.constants = constants;
    }

    public ResponseObjCache createResponseObjCache() {
        return new ResponseObjCache(constants.getTokenExpiryOffset());
    }

}
