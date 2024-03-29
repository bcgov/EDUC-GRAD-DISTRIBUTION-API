package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SchoolStatistic implements Serializable {

    private int transcriptCount;
    private int dogwoodCount;
    private int adultDogwoodCount;
    private int frenchImmersionCount;
    private int programFrancophoneCount;
    private int evergreenCount;
    private int totalCertificateCount;

}
