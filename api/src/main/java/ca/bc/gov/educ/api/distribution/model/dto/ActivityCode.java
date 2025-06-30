package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Getter;

@Getter
public enum ActivityCode {
    MONTHLYDIST("MONTHLYDIST"),
    NONGRADYERUN("NONGRADYERUN"),
    USERDIST("USERDIST"),
    USERDISTRC("USERDISTRC"),
    USERDISTOC("USERDISTOC"),
    SUPPDIST("SUPPDIST"),
    YEARENDDIST("YEARENDDIST");

    private final String value;

    ActivityCode(String value) {
        this.value = value;
    }
}
