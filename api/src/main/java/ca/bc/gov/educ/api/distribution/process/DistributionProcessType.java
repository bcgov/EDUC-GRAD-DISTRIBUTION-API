package ca.bc.gov.educ.api.distribution.process;

public enum DistributionProcessType {
	TYED4 ("TranscriptDataFileYED4Process"),
	CYED2 ("CertificateDataFileYED2Process"),
	CYEDR ("CertificateDataFileYEDRProcess"),
    CYEDB ("CertificateDataFileYEDBProcess");

    private final String value;

    DistributionProcessType(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
