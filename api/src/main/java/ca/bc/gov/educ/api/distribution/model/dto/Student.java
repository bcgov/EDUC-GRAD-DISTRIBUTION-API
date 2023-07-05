package ca.bc.gov.educ.api.distribution.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Student implements Serializable {

    private Pen pen;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String citizenship;
    private LocalDate birthdate;
    private Date lastUpdateDate;
    private Address address;
    private String grade;
    private String gradProgram;
    private String studStatus;
    private String sccDate;
    private String mincodeGrad;
    private String englishCert;
    private String frenchCert;
    //Grad2-1931
    private String consumerEducReqt;

    private String localId;
    private String hasOtherProgram;
    private List<OtherProgram> otherProgramParticipation = new ArrayList<>();

    @JsonDeserialize(as = GraduationData.class)
    private GraduationData graduationData;

    @JsonDeserialize(as = GraduationStatus.class)
    private GraduationStatus graduationStatus = new GraduationStatus();

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

    public String getCitizenship() {
        return citizenship;
    }

    public void setCitizenship(String citizenship) {
        this.citizenship = citizenship;
    }

    @JsonFormat(pattern="yyyy-MM-dd")
    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate value) {
        this.birthdate = value;
    }

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
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

    //Grad2-1931
    public String getConsumerEducReqt() {
        return consumerEducReqt;
    }

    public void setConsumerEducReqt(String consumerEducReqt) {
        this.consumerEducReqt = consumerEducReqt;
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

    public GraduationStatus getGraduationStatus() {
        return graduationStatus;
    }

    public void setGraduationStatus(GraduationStatus graduationStatus) {
        this.graduationStatus = graduationStatus;
    }

    public List<NonGradReason> getNonGradReasons() {
        return nonGradReasons;
    }

    public void setNonGradReasons(List<NonGradReason> nonGradReasons) {
        this.nonGradReasons = nonGradReasons;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(pen, student.pen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pen);
    }
}
