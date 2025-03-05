package ca.bc.gov.educ.api.distribution.model.dto.v2;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component("note")
public class Note {

    private String noteId;
    private String schoolId;
    private String districtId;
    private String independentAuthorityId;
    private String content;
}
