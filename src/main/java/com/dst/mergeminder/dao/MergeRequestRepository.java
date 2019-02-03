package com.dst.mergeminder.dao;

import org.springframework.data.repository.CrudRepository;

import com.dst.mergeminder.dto.MergeRequestModel;

public interface MergeRequestRepository extends CrudRepository<MergeRequestModel, Integer> {
	// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
	// CRUD refers Create, Read, Update, Delete

	MergeRequestModel findFirstByProjectAndMrId(String project, Integer mrId);
}
