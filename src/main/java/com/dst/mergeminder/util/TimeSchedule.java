package com.dst.mergeminder.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeSchedule {

	private static final Logger logger = LoggerFactory.getLogger(TimeSchedule.class);

	private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mma 'ET'");

	private static final Integer DONT_ALERT_BEFORE_HOUR = 8; // 9am
	private static final Integer DONT_ALERT_AFTER_HOUR = 17; // 6pm

	public static boolean shouldAlertNow() {

		LocalDateTime currentDateTime = LocalDateTime.now();
		ZonedDateTime currentEasternTime = currentDateTime.atZone(ZoneId.of("America/New_York"));
		logger.debug("Current Eastern time is: {}", dateFormat.format(currentEasternTime));
		if (currentEasternTime.getDayOfWeek() == DayOfWeek.SATURDAY || currentEasternTime.getDayOfWeek() == DayOfWeek.SUNDAY) {
			logger.debug("It's the weekend.  No notifications.");
			return false;
		}
		if (currentEasternTime.getHour() < DONT_ALERT_BEFORE_HOUR || currentEasternTime.getHour() > DONT_ALERT_AFTER_HOUR) {
			logger.debug("It's too early or too late.  No notifications.");
			return false;
		}

		return true;
	}

}
