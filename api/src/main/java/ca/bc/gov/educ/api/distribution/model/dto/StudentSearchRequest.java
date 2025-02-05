package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSearchRequest implements Serializable {

    private List<UUID> schoolIds = new ArrayList<>();
    private List<UUID> districtIds = new ArrayList<>();
    private List<String> schoolCategoryCodes = new ArrayList<>();
    private List<String> pens = new ArrayList<>();
    private List<String> programs = new ArrayList<>();
    private List<String> reportTypes = new ArrayList<>();

    private String user;
    private Address address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    Date gradDateFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    Date gradDateTo;

    Boolean validateInput;
    String localDownload;
}
