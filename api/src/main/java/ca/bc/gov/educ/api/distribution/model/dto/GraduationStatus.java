package ca.bc.gov.educ.api.distribution.model.dto;

import ca.bc.gov.educ.api.distribution.util.GradLocalDateDeserializer;
import ca.bc.gov.educ.api.distribution.util.GradLocalDateSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GraduationStatus implements Serializable {

    private static final long serialVersionUID = 2L;

    @JsonDeserialize(using = GradLocalDateDeserializer.class)
    @JsonSerialize(using = GradLocalDateSerializer.class)
    private LocalDate programCompletionDate;
    private String honours = "";
    private String gpa = "";
    private String studentGrade = "";
    private String studentStatus = "";
    private String studentStatusName = "";
    private String schoolAtGrad = "";
    private String schoolOfRecord = "";
    private String certificates = "";
    private String graduationMessage = "";
    private String programName = "";

    @JsonDeserialize(using = GradLocalDateDeserializer.class)
    @JsonSerialize(using = GradLocalDateSerializer.class)
    public LocalDate getProgramCompletionDate() {
        return programCompletionDate;
    }

    public void setProgramCompletionDate(LocalDate programCompletionDate) {
        this.programCompletionDate = programCompletionDate;
    }

    public String getHonours() {
        return honours;
    }

    public void setHonours(String honours) {
        this.honours = honours;
    }

    public String getGpa() {
        return gpa;
    }

    public void setGpa(String gpa) {
        this.gpa = gpa;
    }

    public String getStudentGrade() {
        return studentGrade;
    }

    public void setStudentGrade(String studentGrade) {
        this.studentGrade = studentGrade;
    }

    public String getStudentStatus() {
        return studentStatus;
    }

    public void setStudentStatus(String studentStatus) {
        this.studentStatus = studentStatus;
    }

    public String getStudentStatusName() {
        return studentStatusName;
    }

    public void setStudentStatusName(String studentStatusName) {
        this.studentStatusName = studentStatusName;
    }

    public String getSchoolAtGrad() {
        return schoolAtGrad;
    }

    public void setSchoolAtGrad(String schoolAtGrad) {
        this.schoolAtGrad = schoolAtGrad;
    }

    public String getSchoolOfRecord() {
        return schoolOfRecord;
    }

    public void setSchoolOfRecord(String schoolOfRecord) {
        this.schoolOfRecord = schoolOfRecord;
    }

    public String getCertificates() {
        return certificates;
    }

    public void setCertificates(String certificates) {
        this.certificates = certificates;
    }

    public String getGraduationMessage() {
        return graduationMessage;
    }

    public void setGraduationMessage(String graduationMessage) {
        this.graduationMessage = graduationMessage;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }


}

