package com.dst.mergeminder.dao;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.dst.mergeminder.dto.MergeRequestAssignmentInfo;
import com.dst.mergeminder.dto.MergeRequestModel;
import com.dst.mergeminder.dto.MinderProjectsModel;

@Configuration
public class MergeMinderDb {

	private Logger log = LoggerFactory.getLogger(MergeMinderDb.class);

	@Autowired
	MinderProjectsRepository minderProjectsRepository;
	@Autowired
	MergeRequestRepository mergeRequestRepository;

	public void recordMergeRequest(MergeRequestAssignmentInfo mrInfo, long lastNotificationAt) {
		MergeRequestModel mrModel = mergeRequestRepository.findById(mrInfo.getMr().getId()).orElse(null);
		MergeRequestModel newMrModel = updateMrModel(mrModel, mrInfo, lastNotificationAt);

		mergeRequestRepository.save(newMrModel);
	}

	public List<MinderProjectsModel> getMinderProjects() {
		return StreamSupport.stream(minderProjectsRepository.findAll().spliterator(), false).collect(Collectors.toList());
	}

	public MergeRequestModel getMergeRequestModel(int id) {
		return mergeRequestRepository.findById(id).orElse(null);
	}

	/**
	 *
	 * @param mrId the merge request id
	 * @param lastAssignmentId
	 * @return
	 */
	public long getLastReminderSent(int mrId, int lastAssignmentId) {
		MergeRequestModel mrModel = mergeRequestRepository.findById(mrId).orElse(null);
		if (mrModel != null && lastAssignmentId != mrModel.getLastAssignmentId()) {
			// this has been reassigned since we last saw it.  clear the last reminder sent time.
			return -1;
		}
		return (mrModel == null ? -1 : mrModel.getLastReminderSentAt());
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
