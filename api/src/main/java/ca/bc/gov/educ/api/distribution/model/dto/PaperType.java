package ca.bc.gov.educ.api.distribution.model.dto;

import java.io.Serializable;

public class PaperType implements Serializable {

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }
}
