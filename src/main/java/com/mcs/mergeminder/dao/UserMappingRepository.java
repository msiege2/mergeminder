package com.mcs.mergeminder.dao;

import org.springframework.data.repository.CrudRepository;

import com.mcs.mergeminder.dto.UserMappingModel;

public interface UserMappingRepository extends CrudRepository<UserMappingModel, Integer> {

	UserMappingModel findByGitlabUsername(String gitlabUsername);

}
