package com.dst.mergeminder.dao;

import org.springframework.data.repository.CrudRepository;

import com.dst.mergeminder.dto.MinderProjectsModel;

public interface MinderProjectsRepository extends CrudRepository<MinderProjectsModel, Integer> {
	// This will be AUTO IMPLEMENTED by Spring into a Bean called userRepository
	// CRUD refers Create, Read, Update, Delete
}
