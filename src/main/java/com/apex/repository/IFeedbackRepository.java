package com.apex.repository;

import com.apex.entities.Feedback;
import org.springframework.data.repository.CrudRepository;

public interface IFeedbackRepository extends CrudRepository<Feedback, String> {}
