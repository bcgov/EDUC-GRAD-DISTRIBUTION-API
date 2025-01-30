package ca.bc.gov.educ.api.distribution.process;

import lombok.Getter;
@Getter
public enum DistributionProcessType {
    MER ("MergeProcess"),
    RPR ("CreateReprintProcess"),
    BCPR ("CreateBlankCredentialProcess"),
    MERYER ("YearlyMergeProcess"),
    MERSUPP ("SupplementalMergeProcess"),
    PSR ("PostingSchoolReportProcess"),
    PSPR ("PSIReportProcess");

    private final String value;

    DistributionProcessType(String value){
        this.value = value;
    }
}
