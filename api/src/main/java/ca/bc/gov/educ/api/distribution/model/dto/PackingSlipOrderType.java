package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class PackingSlipOrderType {

    private String name;
    private PackingSlipType packingSlipType;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @JsonDeserialize(as = PackingSlipType.class)
    public PackingSlipType getPackingSlipType() {
        return packingSlipType;
    }

    public void setPackingSlipType(PackingSlipType value) {
        this.packingSlipType = value;
    }
}
