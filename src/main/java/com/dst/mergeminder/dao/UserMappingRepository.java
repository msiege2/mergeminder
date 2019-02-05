package com.dst.mergeminder.dao;

import org.springframework.data.repository.CrudRepository;

import com.dst.mergeminder.dto.UserMappingModel;

public interface UserMappingRepository extends CrudRepository<UserMappingModel, Integer> {

	UserMappingModel findByGitlabUsername(String gitlabUsername);

}
