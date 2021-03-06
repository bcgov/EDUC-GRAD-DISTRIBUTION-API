package ca.bc.gov.educ.api.distribution.util;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public class EducDistributionApiUtils {

	private static final Logger logger = LoggerFactory.getLogger(EducDistributionApiUtils.class);
	private static final String ERROR = "ERROR: {}";
	private static final String PARSE_EXP = "Parse Exception {}";

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

	public static String getFileName() {
		Date date = new Date();
		SimpleDateFormat month = new SimpleDateFormat("MMM.dd.yyyy.hh.mm.ss");
		return month.format(date);
	}

	public static String getFileNameSchoolReports(String mincode, int year, String month, String type) {
		return mincode + "_" + year + month + "_" + type;
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
		SimpleDateFormat fromUser = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		SimpleDateFormat myFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		try {
			return myFormat.format(fromUser.parse(updatedTimestamp));
		} catch (ParseException e) {
			logger.debug(PARSE_EXP,e.getLocalizedMessage());
		}
		return updatedTimestamp;

	}

	public static Date formatIssueDateForReportJasper(String updatedTimestamp) {
		SimpleDateFormat fromUser = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		SimpleDateFormat myFormat = new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT);
		try {
			return new SimpleDateFormat(EducDistributionApiConstants.DEFAULT_DATE_FORMAT).parse(myFormat.format(fromUser.parse(updatedTimestamp)));
		} catch (ParseException e) {
			logger.debug(PARSE_EXP,e.getLocalizedMessage());
		}
		return null;

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
