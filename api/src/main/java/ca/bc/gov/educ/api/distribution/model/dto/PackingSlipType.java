package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class PackingSlipType {

    private String name;
    private PaperType paperType;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @JsonDeserialize(as = PaperType.class)
    public PaperType getPaperType() {
        return paperType;
    }

    public void setPaperType(PaperType value) {
        this.paperType = value;
    }
}
