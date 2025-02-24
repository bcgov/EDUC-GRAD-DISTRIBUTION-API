package ca.bc.gov.educ.api.distribution.model.dto.v2;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component("districtAddress")
public class DistrictAddress {

    private String districtAddressId;
    private String districtId;
    private String addressTypeCode;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postal;
    private String provinceCode;
    private String countryCode;
}
