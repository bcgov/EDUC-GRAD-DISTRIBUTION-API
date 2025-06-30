package ca.bc.gov.educ.api.distribution.model.dto.v2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YearEndStudentCredentialDistribution {
    private UUID studentID;
    private String paperType;
    private String certificateTypeCode;
    private String reportingSchoolTypeCode;
}
