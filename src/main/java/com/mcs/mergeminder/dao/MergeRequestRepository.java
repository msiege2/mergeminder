package com.mcs.mergeminder.dao;

import org.springframework.data.repository.CrudRepository;

import com.mcs.mergeminder.dto.MergeRequestModel;

public interface MergeRequestRepository extends CrudRepository<MergeRequestModel, Integer> {
	// This will be AUTO IMPLEMENTED by Spring into a Bean called MergeRequestRepository
	// CRUD refers Create, Read, Update, Delete

	MergeRequestModel findFirstByProjectAndMrId(String project, Integer mrId);
}
