package com.mcs.mergeminder.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.mcs.mergeminder.dto.MinderProjectsModel;

public interface MinderProjectsRepository extends CrudRepository<MinderProjectsModel, Integer> {

	List<MinderProjectsModel> findAllByOrderByNamespaceAscProjectAsc();

	List<MinderProjectsModel> findByNamespaceOrderByProject(String namespace);
}
