package ca.bc.gov.educ.api.distribution.model.dto;

import ca.bc.gov.educ.api.distribution.util.EducDistributionApiConstants;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.Date;

public class PackingSlip implements Serializable {

    String recipient;
    School school;
    PackingSlipOrderType orderType;
    Date orderDate;
    String orderedBy;
    Long orderNumber;
    int quantity;
    int current;
    int total;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    @JsonDeserialize(as = PackingSlipOrderType.class)
    public PackingSlipOrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(PackingSlipOrderType orderType) {
        this.orderType = orderType;
    }

    @JsonFormat(pattern= EducDistributionApiConstants.DEFAULT_DATE_FORMAT)
    public Date getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Date orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderedBy() {
        return orderedBy;
    }

    public void setOrderedBy(String orderedBy) {
        this.orderedBy = orderedBy;
    }

    public Long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
