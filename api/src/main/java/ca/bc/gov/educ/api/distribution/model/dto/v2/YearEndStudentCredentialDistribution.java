package ca.bc.gov.educ.api.distribution.model.dto.v2;

import ca.bc.gov.educ.api.distribution.model.dto.StudentCredentialDistribution;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class YearEndStudentCredentialDistribution extends StudentCredentialDistribution {

    private UUID schoolOfRecordId;
    private UUID schoolAtGradId;
    private UUID districtId;
    private UUID districtAtGradId;

    private String reportingSchoolTypeCode;

    private String transcriptTypeCode;
    private String certificateTypeCode;
}
