package ca.bc.gov.educ.api.distribution.model.dto;

public class TranscriptCourse {

    private String name;
    private String code;
    private String level;
    private String credits;
    private String sessionDate;
    private String type;
    private String relatedCourse;
    private String relatedLevel;
    //Grad2-1931
    private String specialCase;
    //Grad2-2182
    private Boolean isUsed;
    private Double proficiencyScore;
    private String customizedCourseName;
    private Integer originalCredits;
    private String genericCourseType;
    private Integer credit;

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String value) {
        this.code = value;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String value) {
        this.level = value;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String value) {
        this.credits = value;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String value) {
        this.sessionDate = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        this.type = value;
    }

    public String getRelatedCourse() {
        return relatedCourse;
    }

    public void setRelatedCourse(String value) {
        this.relatedCourse = value;
    }

    public String getRelatedLevel() {
        return relatedLevel;
    }

    public void setRelatedLevel(String value) {
        this.relatedLevel = value;
    }

    public String getSpecialCase() {
        return specialCase;
    }

    public void setSpecialCase(String specialCase) {
        this.specialCase = specialCase;
    }

    public Boolean getUsed() {
        return isUsed;
    }

    public void setUsed(Boolean used) {
        isUsed = used;
    }

    public Double getProficiencyScore() {
        return proficiencyScore;
    }

    public void setProficiencyScore(Double proficiencyScore) {
        this.proficiencyScore = proficiencyScore;
    }

    public String getCustomizedCourseName() {
        return customizedCourseName;
    }

    public void setCustomizedCourseName(String customizedCourseName) {
        this.customizedCourseName = customizedCourseName;
    }

    public Integer getOriginalCredits() {
        return originalCredits;
    }

    public void setOriginalCredits(Integer originalCredits) {
        this.originalCredits = originalCredits;
    }

    public String getGenericCourseType() {
        return genericCourseType;
    }

    public void setGenericCourseType(String genericCourseType) {
        this.genericCourseType = genericCourseType;
    }

    public Integer getCredit() {
        return credit;
    }

    public void setCredit(Integer credit) {
        this.credit = credit;
    }
}
