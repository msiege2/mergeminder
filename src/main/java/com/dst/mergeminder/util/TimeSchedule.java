package com.dst.mergeminder.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeSchedule {

	private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
	private static final Logger logger = LoggerFactory.getLogger(TimeSchedule.class);

	private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mma");

	@Value("${mergeminder.beginAlertHour:9}")
	int beginAlertHour;
	@Value("${mergeminder.endAlertHour:18}")
	int endAlertHour;
	@Value("${mergeminder.alertOnWeekends:false}")
	boolean alertOnWeekends;

	public TimeSchedule() {
		// empty constructor
	}

	@PostConstruct
	public void init() {
		ZonedDateTime currentEasternTime = ZonedDateTime.ofInstant(Instant.now(), EASTERN_ZONE);
		logger.info("TimeSchedule initialization complete.  Begin alerting at {}:00, stop alerting at {}:00.  {} alert on weekends. Current Eastern time is: {}",
			beginAlertHour, endAlertHour, alertOnWeekends ? "DO" : "DO NOT", dateFormat.format(currentEasternTime));
	}

	public boolean shouldAlertNow() {
		ZonedDateTime currentEasternTime = ZonedDateTime.ofInstant(Instant.now(), EASTERN_ZONE);
		logger.debug("Current Eastern time is: {}", dateFormat.format(currentEasternTime));
		if (!alertOnWeekends && (currentEasternTime.getDayOfWeek() == DayOfWeek.SATURDAY || currentEasternTime.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			logger.debug("It's the weekend.  No notifications.");
			return false;
		}
		if (currentEasternTime.getHour() < beginAlertHour || currentEasternTime.getHour() > endAlertHour
			|| (currentEasternTime.getHour() == endAlertHour && currentEasternTime.getMinute() != 0)) {
			logger.debug("It's too early or too late.  No notifications.");
			return false;
		}

		return true;
	}

	public boolean shouldPurgeNow() {
		ZonedDateTime currentEasternTime = ZonedDateTime.ofInstant(Instant.now(), EASTERN_ZONE);
		logger.debug("Current Eastern time is: {}", dateFormat.format(currentEasternTime));
		// Only purge at midnight and noon
		return (currentEasternTime.getHour() == 0 || currentEasternTime.getHour() == 12);
	}

	public String currentEasternTime() {
		return dateFormat.format(ZonedDateTime.ofInstant(Instant.now(), EASTERN_ZONE));
	}

}
