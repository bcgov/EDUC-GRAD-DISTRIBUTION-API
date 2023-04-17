package ca.bc.gov.educ.api.distribution.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EducDistributionApiUtils {

	private static final Logger logger = LoggerFactory.getLogger(EducDistributionApiUtils.class);
	private static final String ERROR = "ERROR: {}";
	private static final String PARSE_EXP = "Parse Exception {}";

	public static final String SCHOOL_LABELS_CODE = "000000000";
	public static final String YEARENDDIST = "YEARENDDIST";
	public static final String MONTHLYDIST = "MONTHLYDIST";
	public static final String NONGRADDIST = "NONGRADDIST";
	public static final String SUPPDIST = "SUPPDIST";
	public static final String DISTREP_YE_SD = "DISTREP_YE_SD";
	public static final String DISTREP_YE_SC = "DISTREP_YE_SC";
	public static final String ADDRESS_LABEL_SCHL = "ADDRESS_LABEL_SCHL";
	public static final String ADDRESS_LABEL_DISTRICT = "ADDRESS_LABEL_DIST";
	public static final String ADDRESS_LABEL_YE = "ADDRESS_LABEL_YE";
	public static final String DISTREP_SD = "DISTREP_SD";
	public static final String DISTREP_SC = "DISTREP_SC";
	public static final String NONGRADDISTREP_SC = "NONGRADDISTREP_SC";

	private EducDistributionApiUtils() {}

	public static String formatDate(Date date) {
		if (date == null)
			return null;

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		return simpleDateFormat.format(date);
	}

	public static String formatDate(Date date, String dateFormat) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		return simpleDateFormat.format(date);
	}

	public static Date parseDate(String dateString) {
		if (dateString == null || "".compareTo(dateString) == 0)
			return null;

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		Date date = new Date();

		try {
			date = simpleDateFormat.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return date;
	}

	public static Date parseDate(String dateString, String dateFormat) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		Date date = new Date();

		try {
			date = simpleDateFormat.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return date;
	}

	public static String parseTraxDate(String sessionDate) {
		if (sessionDate == null)
			return null;

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		try {
			Date date = simpleDateFormat.parse(sessionDate);
			LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			return localDate.getYear() + "/" + String.format("%02d", localDate.getMonthValue());

		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static HttpHeaders getHeaders(String accessToken) {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Content-Type", "application/json");
		httpHeaders.setBearerAuth(accessToken);
		return httpHeaders;
	}

	public static HttpHeaders getHeaders (String username,String password)
	{
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		httpHeaders.setBasicAuth(username, password);
		return httpHeaders;
	}

	private static String getFileName() {
		Date date = new Date();
		SimpleDateFormat month = new SimpleDateFormat("MMM.dd.yyyy.hh.mm.ss");
		return month.format(date);
	}

	public static String getFileNameWithMincodeReports(String mincode) {
		return getFileName() + "." + mincode;
	}

	public static String formatDateForReport(String updatedTimestamp) {
		SimpleDateFormat fromUser = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat myFormat = new SimpleDateFormat("yyyyMMdd");
		try {
			return myFormat.format(fromUser.parse(updatedTimestamp));
		} catch (ParseException e) {
			logger.debug(PARSE_EXP,e.getLocalizedMessage());
		}
		return updatedTimestamp;

	}

	public static String formatDateForReportJasper(String updatedTimestamp) {
		if(StringUtils.isNotBlank(updatedTimestamp)) {
			SimpleDateFormat fromUser = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
			SimpleDateFormat myFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
			try {
				return myFormat.format(fromUser.parse(updatedTimestamp));
			} catch (ParseException e) {
				logger.debug(PARSE_EXP,e.getLocalizedMessage());
			}
		}
		return updatedTimestamp;

	}

	public static Date formatIssueDateForReportJasper(String updatedTimestamp) {
		if(StringUtils.isNotBlank(updatedTimestamp)) {
			SimpleDateFormat fromUser = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
			SimpleDateFormat myFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
			try {
				return new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT).parse(myFormat.format(fromUser.parse(updatedTimestamp)));
			} catch (ParseException e) {
				logger.debug(PARSE_EXP, e.getLocalizedMessage());
			}
		}
		return null;

	}

	public static String getDistrictCodeFromMincode(String mincode) {
		return StringUtils.substring(mincode, 0, 3);
	}

	public static String parsingDateForCertificate(String sessionDate) {
		String actualSessionDate = sessionDate + "/01";
		String sDates = null;
		try {
			Date temp = parseDate(actualSessionDate, EducDistributionApiConstants.SECONDARY_DATE_FORMAT);
			sDates = formatDate(temp, EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		} catch (ParseException pe) {
			logger.error(ERROR,pe.getMessage());
		}
		return sDates;
	}

	public static Date parsingTraxDate(String sessionDate) {
		String actualSessionDate = sessionDate + "/01";
		Date sDate = null;
		try {
			Date temp = EducDistributionApiUtils.parseDate(actualSessionDate, EducDistributionApiConstants.SECONDARY_DATE_FORMAT);
			String sDates = EducDistributionApiUtils.formatDate(temp, EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
			sDate = EducDistributionApiUtils.parseDate(sDates, EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		} catch (ParseException pe) {
			logger.error(ERROR,pe.getMessage());
		}
		return sDate;
	}

	public static String parsingNFormating(String inDate) {
		String actualDate = inDate + "/01";
		String sDates = null;
		try {
			Date temp = EducDistributionApiUtils.parseDate(actualDate, EducDistributionApiConstants.SECONDARY_DATE_FORMAT);
			sDates = EducDistributionApiUtils.formatDate(temp, EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		} catch (ParseException pe) {
			logger.error(ERROR,pe.getMessage());
		}
		return sDates;
	}

	public static String getSimpleDateFormat(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		return formatter.format(date);
	}

	public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}
			File[] children = fileToZip.listFiles();
			assert children != null;
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		try (FileInputStream fis = new FileInputStream(fileToZip)) {
			ZipEntry zipEntry = new ZipEntry(fileName);
			zipOut.putNextEntry(zipEntry);
			byte[] bytes = new byte[1024];
			int length;
			while ((length = fis.read(bytes)) >= 0) {
				zipOut.write(bytes, 0, length);
			}
		}catch (Exception e) {
			logger.debug("Write Exception {}",e.getLocalizedMessage());
		}
	}

}
