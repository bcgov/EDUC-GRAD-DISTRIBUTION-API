package ca.bc.gov.educ.api.distribution.model.dto.v2;

import lombok.Data;

import java.io.Serializable;

@Data
public class District implements Serializable {

    private static final long serialVersionUID = 2L;

    String districtNumber;
    String districtName;
    String districtSeq;
    String schoolETPSystem;
    String superIntendent;
    String djdeFlash;
    String activeFlag;
    String address1;
    String address2;
    String city;
    String provCode;
    String countryCode;
    String postal;
}
