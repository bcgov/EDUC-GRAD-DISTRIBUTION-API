package ca.bc.gov.educ.api.distribution.util;

public interface PermissionsContants {
	String _PREFIX = "hasAuthority('";
	String _SUFFIX = "')";

	String GRADUATE_STUDENT = _PREFIX + "SCOPE_UPDATE_GRAD_GRADUATION_STATUS" + _SUFFIX
		+ " and " + _PREFIX + "SCOPE_RUN_GRAD_ALGORITHM" + _SUFFIX;
}
