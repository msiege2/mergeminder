package com.mcs.mergeminder.gitlab;

import com.mcs.mergeminder.dto.MergeRequestAssignmentInfo;
import com.mcs.mergeminder.exception.GitlabIntegrationException;
import com.mcs.mergeminder.properties.GitlabProperties;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.Note;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.models.Assignee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Configuration
public class GitlabIntegration {

	private Logger log = LoggerFactory.getLogger(GitlabIntegration.class);

	private final GitlabProperties gitlabProperties;

	private GitLabApi gitLabApi;

	public GitlabIntegration(GitlabProperties gitlabProperties) {
		this.gitlabProperties = gitlabProperties;
	}

	@PostConstruct
	private void init() throws GitLabApiException {
		log.info("Connecting to GitLab.");
		// Create a GitLabApi instance to communicate with your GitLab server
		gitLabApi = new GitLabApi(gitlabProperties.getUrl(), gitlabProperties.getAccesstoken());
		gitLabApi.getVersion();
		log.info("GitLab connection successful.");
	}

	/**
	 * Goes to gitlab, scrapes open MRs, and creates a list of {@link MergeRequestAssignmentInfo} objects.
	 *
	 * @param namespace
	 * @param projectName
	 * @return
	 * @throws GitLabApiException
	 */
	public Collection<MergeRequestAssignmentInfo> getMergeRequestInfoForProject(String namespace, String projectName) throws GitLabApiException {
		Project project = gitLabApi.getProjectApi().getProject(namespace, projectName);
		if (project == null) {
			log.error("Could not load project {}/{}", namespace, projectName);
			return null;
		}
		// Get open MRs
		List<MergeRequest> mergeRequests = gitLabApi.getMergeRequestApi().getMergeRequests(project.getId(), Constants.MergeRequestState.OPENED);
		List<MergeRequestAssignmentInfo> assignmentInfoList = new ArrayList<>();
		// Populate the AssignmentInfo object for each MR
		for (MergeRequest mr : mergeRequests) {
			String title = mr.getTitle();
			Long mrId = mr.getIid();
			if (ignoreMergeRequest(mr)) {
				log.debug("MR!{}: {} ignored because it contains label that's ignored ( mm.gitlab.ignoredByLabels )", mrId, title);
				continue;
			}
			log.debug("MR!{}: {}", mrId, title);
			// Assignment events are in "notes"
			List<Note> notes = gitLabApi.getNotesApi().getMergeRequestNotes(project.getId(), mr.getIid(), Constants.SortOrder.DESC, Note.OrderBy.CREATED_AT);
			Note lastAssignment = getLastAssignment(notes);
			if (mr.getAssignees() == null || CollectionUtils.isEmpty(mr.getAssignees())) {
				log.info("[{}/{}] MR!{} is not assigned.  Nothing to mind.", namespace, projectName, mrId);
			} else {
				User author = gitLabApi.getUserApi().getUser(mr.getAuthor().getUsername());
				// GitLab allows multiple assignees to a MR, allow for notifying each one.
				for(Assignee user : mr.getAssignees() ) {
					User assignee = gitLabApi.getUserApi().getUser(user.getUsername());
					assignmentInfoList.add(new MergeRequestAssignmentInfo(mr, assignee, author, lastAssignment, namespace, projectName));
				}
			}
		}

		return assignmentInfoList;
	}

	public boolean isMergeRequestMergedOrClosed(String fullyQualifiedProjectName, Long mrId) throws GitlabIntegrationException {
		if (fullyQualifiedProjectName == null || mrId == null) {
			return false;
		}
		log.debug("Looking up MR: [{}] MR id: {}", fullyQualifiedProjectName, mrId);
		try {
			MergeRequest mr = gitLabApi.getMergeRequestApi().getMergeRequest(fullyQualifiedProjectName, mrId);
			if (Constants.MergeRequestState.CLOSED.toString().equals(mr.getState()) ||
				Constants.MergeRequestState.MERGED.toString().equals(mr.getState())) {
				return true;
			}
		} catch (GitLabApiException ex) {
			if (ex.getHttpStatus() == 404) {
				log.debug("Cannot find this merge request.  Assuming it no longer exists!");
				return true;
			}
			throw new GitlabIntegrationException("Problem looking up merge request.");
		}

		return false;
	}

	private boolean ignoreMergeRequest(MergeRequest mergeRequest) {
		List<String> mergeRequestLabels = Optional.ofNullable(mergeRequest.getLabels()).orElse(List.of());
		boolean ignoreMergeRequest = false;
		for (String label : mergeRequestLabels) {
			ignoreMergeRequest = Optional.ofNullable(gitlabProperties.getIgnoredByLabels()).orElse(List.of())
					.stream()
					.filter(ignoreByLabel -> StringUtils.equalsIgnoreCase(StringUtils.trim(ignoreByLabel), StringUtils.trim(label)))
					.map(match -> true)
					.findAny()
					.orElse(false);
			if (ignoreMergeRequest) {
				break;
			}
		}

		return ignoreMergeRequest;
	}

	/**
	 * Parse all the notes for the MR and return the most recent assignment.
	 *
	 * @param notes List of {@link Note}.  Assumes these are in descending order of create date.
	 * @return Date at which the most recent assignment took place, or null if none was found.
	 */
	private Note getLastAssignment(List<Note> notes) {
		if (CollectionUtils.isEmpty(notes)) {
			return null;
		}
		for (Note note : notes) {
			if (note.getBody().toLowerCase().startsWith("assigned to")) {
				// this is an assignment.  Since the notes are ordered by create date desc, this is the most recent assignment.
				return note;
			}
			log.debug("Note: {}, at {}", note.getBody(), note.getCreatedAt());
		}
		return null;
	}
}
