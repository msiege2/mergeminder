package com.dst.mergeminder.dao;

import java.util.Date;
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
	 * @param id the merge request id
	 * @return
	 */
	public long getLastReminderSent(int id, Date assignedAt) {
		MergeRequestModel mrModel = mergeRequestRepository.findById(id).orElse(null);
		// give a 20 second buffer between the assigned at time in the MR and the time in the db.  I don't
		// know why these don't always come out the same, but I don't really care to figure it out right now.
		if (mrModel != null && assignedAt != null && assignedAt.before(new Date(mrModel.getAssignedAt().getTime() - 20000))) {
			// this has been reassigned since we last saw it.  clear the reminder time.
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
		newMergeRequestModel.setAssignedAt(mrInfo.getAssignedAt());

		return newMergeRequestModel;
	}

}
