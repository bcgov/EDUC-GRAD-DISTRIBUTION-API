package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Student implements Serializable {

    private Pen pen;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private Date birthdate;
    private Address address;
    private String grade;
    private String gradProgram;
    private String studStatus;
    private String sccDate;
    private String mincodeGrad;
    private String englishCert;
    private String frenchCert;

    private String localId;
    private String hasOtherProgram;
    private List<OtherProgram> otherProgramParticipation = new ArrayList<>();

    @JsonDeserialize(as = GraduationData.class)
    private GraduationData graduationData;

    private List<NonGradReason> nonGradReasons = new ArrayList<>();

    @JsonDeserialize(as = Pen.class)
    public Pen getPen() {
        return pen;
    }

    public void setPen(Pen value) {
        this.pen = value;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String value) {
        this.firstName = value;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String value) {
        this.lastName = value;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String value) {
        this.gender = value;
    }

    @JsonFormat(pattern="yyyy-MM-dd")
    public Date getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(Date value) {
        this.birthdate = value;
    }

    @JsonDeserialize(as = Address.class)
    public Address getAddress() {
        return address;
    }

    public void setAddress(Address value) {
        this.address = value;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String value) {
        this.grade = value;
    }

    public String getStudStatus() {
        return studStatus;
    }

    public void setStudStatus(String value) {
        this.studStatus = value;
    }

    public String getSccDate() {
        return sccDate;
    }

    public void setSccDate(String value) {
        this.sccDate = value;
    }

    public String getMincodeGrad() {
        return mincodeGrad;
    }

    public void setMincodeGrad(String value) {
        this.mincodeGrad = value;
    }

    public String getEnglishCert() {
        return englishCert;
    }

    public void setEnglishCert(String value) {
        this.englishCert = value;
    }

    public String getFrenchCert() {
        return frenchCert;
    }

    public void setFrenchCert(String value) {
        this.frenchCert = value;
    }

    public String getGradProgram() {
        return gradProgram;
    }

    public void setGradProgram(String gradProgram) {
        this.gradProgram = gradProgram;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getHasOtherProgram() {
        return hasOtherProgram;
    }

    public void setHasOtherProgram(String hasOtherProgram) {
        this.hasOtherProgram = hasOtherProgram;
    }

    public List<OtherProgram> getOtherProgramParticipation() {
        return otherProgramParticipation;
    }

    public void setOtherProgramParticipation(List<OtherProgram> otherProgramParticipation) {
        this.otherProgramParticipation = otherProgramParticipation;
    }

    public GraduationData getGraduationData() {
        return graduationData;
    }

    public void setGraduationData(GraduationData graduationData) {
        this.graduationData = graduationData;
    }

    public List<NonGradReason> getNonGradReasons() {
        return nonGradReasons;
    }

    public void setNonGradReasons(List<NonGradReason> nonGradReasons) {
        this.nonGradReasons = nonGradReasons;
    }
}
