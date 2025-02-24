package ca.bc.gov.educ.api.distribution.model.dto.v2;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component("districtContact")
public class DistrictContact {

    private String districtContactId;
    private String districtId;
    private String districtContactTypeCode;
    private String phoneNumber;
    private String jobTitle;
    private String phoneExtension;
    private String alternatePhoneNumber;
    private String alternatePhoneExtension;
    private String email;
    private String firstName;
    private String lastName;
    private String effectiveDate;
    private String expiryDate;
}
