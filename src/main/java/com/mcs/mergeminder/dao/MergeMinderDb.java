package com.mcs.mergeminder.dao;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.mcs.mergeminder.dto.MergeRequestAssignmentInfo;
import com.mcs.mergeminder.dto.MergeRequestModel;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.dto.UserMappingModel;

@Configuration
public class MergeMinderDb {

	private Logger log = LoggerFactory.getLogger(MergeMinderDb.class);

	private final MinderProjectsRepository minderProjectsRepository;
	private final MergeRequestRepository mergeRequestRepository;
	private final UserMappingRepository userMappingRepository;

	public MergeMinderDb(MinderProjectsRepository minderProjectsRepository, MergeRequestRepository mergeRequestRepository, UserMappingRepository userMappingRepository) {
		this.minderProjectsRepository = minderProjectsRepository;
		this.mergeRequestRepository = mergeRequestRepository;
		this.userMappingRepository = userMappingRepository;
	}

	// Merge Request Models
	////////////////////////
	public MergeRequestModel recordMergeRequest(MergeRequestAssignmentInfo mrInfo, long lastNotificationAt) {
		MergeRequestModel mrModel = mergeRequestRepository.findById(mrInfo.getMr().getId()).orElse(null);
		MergeRequestModel newMrModel = updateMrModel(mrModel, mrInfo, lastNotificationAt);

		return mergeRequestRepository.save(newMrModel);
	}

	public List<MergeRequestModel> getAllMergeRequestModels() {
		return StreamSupport.stream(mergeRequestRepository.findAll().spliterator(), false).collect(Collectors.toList());
	}

	public MergeRequestModel getMergeRequestModel(long id) {
		return mergeRequestRepository.findById(id).orElse(null);
	}

	public void removeMergeRequestModel(MergeRequestModel model) {
		mergeRequestRepository.delete(model);
	}

	/**
	 * @param mrId the merge request id
	 * @param lastAssignmentId
	 * @return
	 */
	public long getLastReminderSent(long mrId, long lastAssignmentId) {
		MergeRequestModel mrModel = mergeRequestRepository.findById(mrId).orElse(null);
		if (mrModel != null && lastAssignmentId != mrModel.getLastAssignmentId()) {
			// this has been reassigned since we last saw it.  clear the last reminder sent time.
			return -1;
		}
		return (mrModel == null ? -1 : mrModel.getLastReminderSentAt());
	}

	// Project Models
	//////////////////
	public List<MinderProjectsModel> getMinderProjects() {
		return StreamSupport.stream(minderProjectsRepository.findAllByOrderByNamespaceAscProjectAsc().spliterator(), false).collect(Collectors.toList());
	}

	public List<MinderProjectsModel> getMinderProjectsForNamespace(String namespace) {
		return StreamSupport.stream(minderProjectsRepository.findByNamespaceOrderByProject(namespace).spliterator(), false).collect(Collectors.toList());
	}

	public MinderProjectsModel saveMinderProject(MinderProjectsModel project) {
		if (project != null) {
			return minderProjectsRepository.save(project);
		}
		return null;
	}

	public void removeMinderProject(Integer id) {
		minderProjectsRepository.delete(minderProjectsRepository.findById(id).orElse(null));
	}

	public void removeMinderProject(MinderProjectsModel project) {
		minderProjectsRepository.delete(project);
	}

	// User Mapping Models
	///////////////////////
	public List<UserMappingModel> getAllUserMappings() {
		return StreamSupport.stream(userMappingRepository.findAll().spliterator(), false).collect(Collectors.toList());
	}

	public UserMappingModel getUserMappingById(Integer id) {
		return userMappingRepository.findById(id).orElse(null);
	}

	public UserMappingModel getUserMappingByGitlabUsername(String gitlabUsername) {
		return userMappingRepository.findByGitlabUsername(gitlabUsername);
	}

	public UserMappingModel saveUserMapping(UserMappingModel mapping) {
		if (mapping != null) {
			return userMappingRepository.save(mapping);
		}
		return null;
	}

	private MergeRequestModel updateMrModel(MergeRequestModel mrModel, MergeRequestAssignmentInfo mrInfo, long lastNotificationAt) {
		MergeRequestModel newMergeRequestModel = new MergeRequestModel(mrModel);

		newMergeRequestModel.setId(mrInfo.getMr().getId());
		newMergeRequestModel.setProject(mrInfo.getFullyQualifiedProjectName());
		newMergeRequestModel.setMrId(mrInfo.getMr().getIid());
		newMergeRequestModel.setAssignee(mrInfo.getAssignee().getName());
		newMergeRequestModel.setAssigneeEmail(mrInfo.getAssignee().getEmail());
		newMergeRequestModel.setLastReminderSentAt(lastNotificationAt);
		newMergeRequestModel.setLastAssignmentId(mrInfo.getLastAssignmentId());
		newMergeRequestModel.setAssignedAt(mrInfo.getAssignedAt());

		return newMergeRequestModel;
	}

}
