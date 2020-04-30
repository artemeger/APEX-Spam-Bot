package com.apex.repository;

import com.apex.entities.Feedback;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface IFeedbackRepository extends CrudRepository<Feedback, Long> {
    public Optional<Feedback> findFirstByUserId(final int userId);
}
