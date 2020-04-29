package com.apex.repository;

import com.apex.entities.TGUser;
import org.springframework.data.repository.CrudRepository;

public interface ITGUserRepository extends CrudRepository<TGUser, Integer> {
}
