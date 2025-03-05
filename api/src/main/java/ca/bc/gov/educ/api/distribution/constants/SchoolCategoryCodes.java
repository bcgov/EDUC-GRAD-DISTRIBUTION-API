package ca.bc.gov.educ.api.distribution.constants;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * The enum for school category codes
 */
@Getter
public enum SchoolCategoryCodes {
    IMM_DATA("IMM_DATA"),
    CHILD_CARE("CHILD_CARE"),
    MISC("MISC"),
    PUBLIC("PUBLIC"),
    INDEPEND("INDEPEND"),
    FED_BAND("FED_BAND"),
    OFFSHORE("OFFSHORE"),
    EAR_LEARN("EAR_LEARN"),
    YUKON("YUKON"),
    POST_SEC("POST_SEC"),
    INDP_FNS("INDP_FNS");

    private final String code;
    SchoolCategoryCodes(String code) { this.code = code; }

    public static List<String> getSchoolTypesWithoutDistricts() {
        List<String> codes = new ArrayList<>();
        codes.add(INDP_FNS.getCode());
        codes.add(OFFSHORE.getCode());
        codes.add(FED_BAND.getCode());
        codes.add(INDEPEND.getCode());
        return codes;
    }

}
