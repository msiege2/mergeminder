package com.mcs.mergeminder.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;


@EnableConfigurationProperties
@ConfigurationProperties(prefix = "mm")
@Validated
public class MergeMinderProperties {

	private String applicationVersion;
	private Boolean scheduleBypass = false;
	private List<String> emailDomains = null;
	private int beginAlertHour = 9;
	private int endAlertHour = 18;
	private Boolean alertOnWeekends = false;

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public Boolean getScheduleBypass() {
		return scheduleBypass;
	}

	public void setScheduleBypass(Boolean scheduleBypass) {
		this.scheduleBypass = scheduleBypass;
	}

	public List<String> getEmailDomains() {
		return emailDomains;
	}

	public void setEmailDomains(List<String> emailDomains) {
		this.emailDomains = emailDomains;
	}

	public int getBeginAlertHour() {
		return beginAlertHour;
	}

	public void setBeginAlertHour(int beginAlertHour) {
		this.beginAlertHour = beginAlertHour;
	}

	public int getEndAlertHour() {
		return endAlertHour;
	}

	public void setEndAlertHour(int endAlertHour) {
		this.endAlertHour = endAlertHour;
	}

	public Boolean getAlertOnWeekends() {
		return alertOnWeekends;
	}

	public void setAlertOnWeekends(Boolean alertOnWeekends) {
		this.alertOnWeekends = alertOnWeekends;
	}
}
