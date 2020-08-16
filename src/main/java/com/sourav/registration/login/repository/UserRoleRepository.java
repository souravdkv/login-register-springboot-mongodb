package com.sourav.registration.login.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.sourav.registration.login.model.RoleEnum;
import com.sourav.registration.login.model.UserRole;

public interface UserRoleRepository extends MongoRepository<UserRole, String> {

	Optional<UserRole> findByName(RoleEnum name);
}