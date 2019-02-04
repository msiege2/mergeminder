package com.dst.mergeminder;

/**
 * Enum representing the time periods at which notifications are send.  Note, this
 * is not the amount of time the MR has been sitting, just the thresholds at
 * which we create new alerts/messages.
 */
public enum ReminderLength {
	INITIAL_REMINDER(0, true),
	TWO_HOURS(2, false),
	FOUR_HOURS(4, true),
	SIX_HOURS(6, false),
	EIGHT_HOURS(8, true),
	TWELVE_HOURS(12, true),
	ONE_DAY(24, true),
	TWO_DAYS(48, true);

	private long hours;
	private boolean sendAlert;

	ReminderLength(long hours, boolean sendAlert) {
		this.hours = hours;
		this.sendAlert = sendAlert;
	}

	/**
	 * Numeric number of hours associated with the ReminderLength.
	 * @return
	 */
	public long getHours() {
		return hours;
	}

	/**
	 * Flag to determine if alerts should be sent at the given threshold.
	 * @return
	 */
	public boolean shouldSendAlert() {
		return sendAlert;
	}

	/**
	 * Get the ReminderLength corresponding to the number of hours passed in.
	 * @param hoursSinceAssignment the number of hours the MR has been assigned.
	 * @return
	 */
	public static ReminderLength getLastReminderPeriod(long hoursSinceAssignment) {
		if (hoursSinceAssignment >= TWO_DAYS.getHours()) {
			return TWO_DAYS;
		}
		if (hoursSinceAssignment >= ONE_DAY.getHours()) {
			return ONE_DAY;
		}
		if (hoursSinceAssignment >= TWELVE_HOURS.getHours()) {
			return TWELVE_HOURS;
		}
		if (hoursSinceAssignment >= EIGHT_HOURS.getHours()) {
			return EIGHT_HOURS;
		}
		if (hoursSinceAssignment >= SIX_HOURS.getHours()) {
			return SIX_HOURS;
		}
		if (hoursSinceAssignment >= FOUR_HOURS.getHours()) {
			return FOUR_HOURS;
		}
		if (hoursSinceAssignment >= TWO_HOURS.getHours()) {
			return TWO_HOURS;
		}
		return INITIAL_REMINDER;
	}

	/**
	 * Gets the message to send in a PM over slack.
	 * @param assigneeFirstName
	 * @param mrName
	 * @param fullyQualifiedProjectName
	 * @param authorName
	 * @return
	 */
	public String getSlackPrivateNotificationMessage(String assigneeFirstName, String mrName, String fullyQualifiedProjectName, String authorName) {
		switch (this) {
			case INITIAL_REMINDER:
				return ReminderMessages.INITIAL_REMINDER_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case TWO_HOURS:
				return ReminderMessages.TWO_HOURS_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case FOUR_HOURS:
				return ReminderMessages.FOUR_HOURS_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case SIX_HOURS:
				return ReminderMessages.SIX_HOURS_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case EIGHT_HOURS:
				return ReminderMessages.EIGHT_HOURS_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case TWELVE_HOURS:
				return ReminderMessages.TWELVE_HOURS_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case ONE_DAY:
				return ReminderMessages.ONE_DAY_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
			case TWO_DAYS:
			default:
				return ReminderMessages.TWO_DAYS_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%AUTHOR_NAME%", authorName);
		}
	}

	/**
	 * Gets the message to send in a PM if the assignee is also the author.
	 * @param assigneeFirstName
	 * @param mrName
	 * @param fullyQualifiedProjectName
	 * @return
	 */
	public String getReminderForAuthor(String assigneeFirstName, String mrName, String fullyQualifiedProjectName) {
		return ReminderMessages.AUTHOR_REMINDER_PM.replaceAll("%F_NAME%", assigneeFirstName).replaceAll("%MRNAME%", mrName).replaceAll("%FQPN%", fullyQualifiedProjectName).replaceAll("%HOURS%", Long.toString(this.hours));
	}

	/**
	 * Reminder messages to send out on slack.
	 */
	private class ReminderMessages {

		private static final String AUTHOR_REMINDER_PM = "Hey %F_NAME%.  I just wanted to let you know that your merge request %MRNAME% in [%FQPN%] is assigned"
			+ " to you.  It has been assigned to you for around %HOURS% hours.  Please don't forget about it!";

		private static final String INITIAL_REMINDER_PM = "Hey %F_NAME%!  I just wanted to let you know that you've been assigned a "
			+ "merge request -- %MRNAME%.  It was created by %AUTHOR_NAME% in project [%FQPN%].  Please do your best to take a look at it when you can!";

		private static final String TWO_HOURS_PM = "Hey there, %F_NAME%!  I see that merge request (%MRNAME%) in project [%FQPN%]"
			+ " has been sitting there for a couple of hours.  Please do your best to take a look at it when you can.";

		private static final String FOUR_HOURS_PM = "Hey there, %F_NAME%!  I see that merge request (%MRNAME%) in project [%FQPN%]"
			+ " has been assigned to you for four hours now.  Help %AUTHOR_NAME% out and take a look when you have a chance.";

		private static final String SIX_HOURS_PM = "Hey there, %F_NAME%!  I see that merge request (%MRNAME%) in project [%FQPN%]"
			+ " has been assigned to you for six hours now.  Help %AUTHOR_NAME% out and take a look when you have a chance.";

		private static final String EIGHT_HOURS_PM = "%F_NAME%, %F_NAME%, %F_NAME%.  I see that merge request (%MRNAME%) in project [%FQPN%]"
			+ " has been assigned to you for eight hours now.  Help %AUTHOR_NAME% out and review it as soon as you can.";

		private static final String TWELVE_HOURS_PM = "%F_NAME%, I see that merge request (%MRNAME%) in project [%FQPN%]"
			+ " has been assigned to you for over twelve hours.  %AUTHOR_NAME% probably wants to get this thing closed.  Please take a look ASAP.  Thanks.";

		private static final String ONE_DAY_PM = "%F_NAME%, I see %MRNAME% in project [%FQPN%]"
			+ " has been assigned to you for over a day!  They don't call me MergeMinder for nothing!  Let's get this closed.  Please take a look ASAP.  Thank you.";

		private static final String TWO_DAYS_PM = "Come on, %F_NAME%!  I see that %MRNAME% in project [%FQPN%]"
			+ " has been assigned to you for over two days.  Please take a look at this and resolve it ASAP.  Thanks.";

	}
}

