package com.mcs.mergeminder.slack.conversations;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.UserMappingModel;
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.slack.ExtendedConversation;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public class SetUnmappedUserConversation extends ExtendedConversation {

	public enum ConversationState {
		INITIALIZED,
		SELECTING_MAPPING_TO_UPDATE,
		INPUTTING_MAPPING_DATA
	}

	private ConversationState conversationState;

	List<UserMappingModel> loadedUnmappedUsers = null;
	UserMappingModel mappingToUpdate = null;

	public SetUnmappedUserConversation(MergeMinderDb mergeMinderDb) {
		super(mergeMinderDb);
		this.conversationState = SetUnmappedUserConversation.ConversationState.INITIALIZED;
	}

	@Override
	public void start(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			List<UserMappingModel> userMappings = mergeMinderDb.getAllUserMappings();
			if (userMappings == null) {
				throw new ConversationException("Could not load user mappings.");
			}
			List<UserMappingModel> unmappedUsers = userMappings.stream().filter(c -> c.getSlackUID() == null && c.getSlackEmail() == null).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(unmappedUsers)) {
				simulateHumanStyleMessageSending(channel, "There are currently no unmapped users in the MergeMinder database.", session);
				finished = true;
				return;
			}
			this.loadedUnmappedUsers = unmappedUsers;
			simulateHumanStyleMessageSending(channel, "Here are the users from Gitlab [username|realname] that are currently unmapped in Slack:", session);
			for (int i = 0; i < unmappedUsers.size(); i++) {
				StringBuilder message = new StringBuilder();
				message.append("   ");
				message.append(i + 1);
				message.append(".  [ *");
				message.append(unmappedUsers.get(i).getGitlabUsername());
				message.append("*  |  *");
				message.append(unmappedUsers.get(i).getGitlabName());
				message.append("* ]");
				session.sendMessage(channel, message.toString());
			}
			session.sendMessage(channel, "Which line number would you like to map? (Type \"exit\" to cancel)");
			conversationState = ConversationState.SELECTING_MAPPING_TO_UPDATE;
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in starting SET UNMAPPED USERS conversation.", e);
		}
	}

	@Override
	public void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			if ("exit".equalsIgnoreCase(userInput.trim())) {
				simulateHumanStyleMessageSending(channel, "Cancelling SET UNMAPPED USER operation.", session);
				finished = true;
				return;
			}
			switch (conversationState) {
				case SELECTING_MAPPING_TO_UPDATE:
					selectMappingToUpdate(channel, messageSender, session, userInput);
					break;
				case INPUTTING_MAPPING_DATA:
					updateMapping(channel, messageSender, session, userInput);
					break;
				default:
					throw new ConversationException("Unknown state in SET UNMAPPED USER conversation!");
			}
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in SET UNMAPPED USERS conversation.", e);
		}
	}

	private void selectMappingToUpdate(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) {
		try {
			int mappingNumberToUpdate = Integer.parseInt(userInput);
			if (mappingNumberToUpdate <= 0 || mappingNumberToUpdate > loadedUnmappedUsers.size()) {
				simulateHumanStyleMessageSending(channel, "Please enter a number corresponding to a line number above.  (Type \"exit\" to cancel)", session);
			} else {
				mappingToUpdate = loadedUnmappedUsers.get(mappingNumberToUpdate - 1);
				conversationState = ConversationState.INPUTTING_MAPPING_DATA;
				simulateHumanStyleMessageSending(channel, "Ok.  Enter either the user's internal slack ID (U########) or Slack email address. (Type \"exit\" to cancel)", session);
			}
		} catch (NumberFormatException e) {
			simulateHumanStyleMessageSending(channel, "That doesn't seem right.  Please enter a line number to map?  (Type \"exit\" to cancel)", session);
		}
	}

	private void updateMapping(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			if (mappingToUpdate == null) {
				throw new ConversationException("Cannot find mapping to update.");
			}
			userInput = parseOutEmailFromLink(userInput);
			StringBuilder message = new StringBuilder();
			message.append("Great!\n I have updated [ *");
			message.append(mappingToUpdate.getGitlabUsername());
			message.append("*  |  *");
			message.append(mappingToUpdate.getGitlabName());
			message.append("* ] with ");
			if (userInput.contains("@")) {
				// this is assumed to be an email address
				mappingToUpdate.setSlackEmail(userInput.trim());
				mergeMinderDb.saveUserMapping(mappingToUpdate);
				message.append("Slack email ");
				message.append(userInput.trim());
				simulateHumanStyleMessageSending(channel, message.toString(), session);
				finished = true;
			} else {
				// this is assumed to be a slack ID
				if (userInput.toUpperCase().startsWith("U")) {
					mappingToUpdate.setSlackUID(userInput.toUpperCase().trim());
					mergeMinderDb.saveUserMapping(mappingToUpdate);
					message.append("Slack UID ");
					message.append(userInput.toUpperCase().trim());
					simulateHumanStyleMessageSending(channel, message.toString(), session);
					finished = true;
				} else {
					simulateHumanStyleMessageSending(channel, "That doesn't seem right.  Enter either the user's internal slack ID (U########) or Slack email address. (Type \"exit\" to cancel)", session);
				}
			}
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in SET UNMAPPED USERS conversation.", e);
		}
	}

	private String parseOutEmailFromLink(String userInput) {
		try {
			userInput = userInput.trim();
			// try to match an email link that Slack converted
			Pattern regex = Pattern.compile("<mailto:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\|([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+)>");
			Matcher regexMatcher = regex.matcher(userInput);
			if (regexMatcher.find()) {
				return regexMatcher.group(1);
			}
		} catch (Exception ex) {
			// Ignored
		}
		return userInput;
	}
}
