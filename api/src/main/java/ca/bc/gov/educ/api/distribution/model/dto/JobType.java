package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Getter;

@Getter
public enum JobType {
    MER("MER"),
    MERYER("MERYER"),
    MERSUPP("MERSUPP");

    private final String value;

    JobType(String value) {
        this.value = value;
    }
}
