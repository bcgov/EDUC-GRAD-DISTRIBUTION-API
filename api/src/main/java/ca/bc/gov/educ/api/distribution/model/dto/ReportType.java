package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Getter;

@Getter
public enum ReportType {
    DISTREP_SD ("DISTREP_SD"),
    DISTREP_SC ("DISTREP_SC"),
    NONGRADDISTREP_SC ("NONGRADDISTREP_SC"),
    NONGRADDISTREP_SD ("NONGRADDISTREP_SD"),
    DISTREP_YE_SD ("DISTREP_YE_SD"),
    DISTREP_YE_SC ("DISTREP_YE_SC");

    private final String value;

    ReportType(String value) {
        this.value = value;
    }
}
