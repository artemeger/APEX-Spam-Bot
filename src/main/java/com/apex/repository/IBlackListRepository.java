package com.apex.repository;

import com.apex.entities.Blacklist;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface IBlackListRepository extends CrudRepository<Blacklist, String> {

    public Optional<Blacklist> findFirstByHash(final String hash);

}
