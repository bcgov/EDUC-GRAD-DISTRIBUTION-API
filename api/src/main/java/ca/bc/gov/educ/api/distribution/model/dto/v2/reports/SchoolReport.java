package ca.bc.gov.educ.api.distribution.model.dto.v2.reports;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class SchoolReport extends BaseReport {
	private UUID schoolOfRecordId;
	private String schoolName;
	private String schoolCategory;
}
