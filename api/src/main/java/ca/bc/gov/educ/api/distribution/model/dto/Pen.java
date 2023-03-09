package ca.bc.gov.educ.api.distribution.model.dto;

import java.io.Serializable;
import java.util.Objects;


public class Pen implements Serializable {

    private String pen;
    private String entityID;

    public String getPen() {
        return pen;
    }

    public void setPen(String value) {
        this.pen = value;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String value) {
        this.entityID = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pen pen1 = (Pen) o;
        return Objects.equals(pen, pen1.pen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pen);
    }
}
