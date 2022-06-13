package ca.bc.gov.educ.api.distribution.process;

public enum DistributionProcessType {
    MER ("MergeProcess"),
    RPR ("CreateReprintProcess"),
    BCPR ("CreateBlankCredentialProcess");

    private final String value;

    DistributionProcessType(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
