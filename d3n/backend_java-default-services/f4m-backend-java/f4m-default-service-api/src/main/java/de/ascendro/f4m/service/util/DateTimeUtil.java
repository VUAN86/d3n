package de.ascendro.f4m.service.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateTimeUtil {
	//http://json-schema.org/latest/json-schema-validation.html
	//https://tools.ietf.org/html/rfc3339
	public static final String JSON_DATE_FORMAT = "yyyy-MM-dd";
	public static final String JSON_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	public static final String DATETIME_WITH_MILLIS_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
	public static final long MILLI_TO_NANOSECONDS = 1000000;
	
	public static final DateTimeFormatter TIMESTAMP_ISO_FORMAT = DateTimeFormatter.ofPattern(DATETIME_WITH_MILLIS_FORMAT);
	public static final DateTimeFormatter DATE_ISO_FORMAT = DateTimeFormatter.ofPattern(JSON_DATE_FORMAT);

	public static final ZoneOffset TIMEZONE = ZoneOffset.UTC;

	private static final DateTimeFormatter ANY_DATE_FORMAT_PARSER = new DateTimeFormatterBuilder() //
			.parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE) // date
			.optionalStart().appendLiteral('T').append(DateTimeFormatter.ISO_LOCAL_TIME) // time
			.optionalStart().optionalStart().appendOffsetId().optionalEnd() // full zone
			.optionalStart().appendOffset("+HH", "Z").optionalEnd() // short zone
			.optionalStart().appendLiteral('[').parseCaseSensitive().appendZoneRegionId().appendLiteral(']') // zone text
			.optionalEnd().optionalEnd().optionalEnd() //
			.toFormatter();

	private DateTimeUtil(){
	}

	public static String getCurrentTimestampInISOFormat() {
		return formatISODateTime(ZonedDateTime.now(TIMEZONE));
	}

	public static ZonedDateTime getCurrentDateTime() {
		return ZonedDateTime.now(TIMEZONE);
	}
	
	public static ZonedDateTime getCurrentDateStart() {
		return ZonedDateTime.of(ZonedDateTime.now(TIMEZONE).toLocalDate(), LocalTime.MIN, ZoneOffset.UTC);
	}

	public static ZonedDateTime getCurrentDateEnd() {
		return ZonedDateTime.of(ZonedDateTime.now(TIMEZONE).toLocalDate(), LocalTime.MAX, ZoneOffset.UTC);
	}

	public static long getUTCTimestamp() {
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
		return calendar.getTimeInMillis();
	}
	
	public static long getUTCDateTimestamp() {
		final Calendar calendar = GregorianCalendar.getInstance();
		
		calendar.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		return calendar.getTimeInMillis();
	}
	
	public static long getOneDayInMillis() {
		return 24 * 60 * 60 * 1000L;
	}

	public static ZonedDateTime parseISODateTimeString(String isoDateTimeString) {
		return isoDateTimeString == null ? null : deserializeDate(isoDateTimeString);
	}
	
	public static String formatISODateTime(ZonedDateTime dateTime){
		return dateTime == null ? null : dateTime.format(TIMESTAMP_ISO_FORMAT);
	}
	
	public static String formatISODate(LocalDate date){
		return date == null ? null : date.format(DATE_ISO_FORMAT);
	}
	
	public static boolean isCurrentBetween(ZonedDateTime start, ZonedDateTime end) {
		boolean result;
		if (start != null && end != null) {
			ZonedDateTime now = getCurrentDateTime();
			result = now.isAfter(start) && now.isBefore(end);
		} else {
			result = false;
		}
		return result;
	}

	public static long getSecondsBetween(Temporal start, Temporal end) {
		return Duration.between(start, end).getSeconds();
	}

	private static ZonedDateTime deserializeDate(String dateSting) {
		ZonedDateTime value;
		TemporalAccessor temporalAccessor = ANY_DATE_FORMAT_PARSER.parseBest(dateSting, ZonedDateTime::from,
				LocalDateTime::from, LocalDate::from);
		if (temporalAccessor instanceof ZonedDateTime) {
			value = (ZonedDateTime) temporalAccessor;
		} else if (temporalAccessor instanceof LocalDateTime) {
			value = ((LocalDateTime) temporalAccessor).atZone(DateTimeUtil.TIMEZONE);
		} else {
			value = ((LocalDate) temporalAccessor).atStartOfDay(DateTimeUtil.TIMEZONE);
		}
		return value.withZoneSameInstant(ZoneOffset.UTC);
	}

}
