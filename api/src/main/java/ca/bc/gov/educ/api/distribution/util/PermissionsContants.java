package ca.bc.gov.educ.api.distribution.util;

public interface PermissionsContants {
	String _PREFIX = "#oauth2.hasAnyScope('";
	String _SUFFIX = "')";

	String GRADUATE_STUDENT = _PREFIX + "UPDATE_GRAD_GRADUATION_STATUS', 'RUN_GRAD_ALGORITHM" + _SUFFIX;
}
