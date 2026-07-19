package com.nulla.donghaeguard.repository;

import com.nulla.donghaeguard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}