package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.springframework.stereotype.Component;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.Map;

@Data
@Component
@XmlType(name = "")
@XmlRootElement(name = "generateReport")
@XmlSeeAlso({
		School.class,
})
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = School.class),
})
public class ReportData implements Serializable {

	@JsonDeserialize(as = School.class)
	private School school;
	@JsonFormat(pattern="yyyy-MM-dd")
	private String updateDate;
	@JsonDeserialize(as = PackingSlip.class)
	private PackingSlip packingSlip;

	private Map<String, String> parameters;
}
