package ca.bc.gov.educ.api.distribution.model.dto.v2.reports;

import ca.bc.gov.educ.api.distribution.model.dto.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseReport extends BaseModel {
  private UUID id;
  private String report;
  private String reportTypeCode;
  private String reportTypeLabel;
}
