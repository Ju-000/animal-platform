package com.animalplatform.chat.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatQuickAnswerRepository extends JpaRepository<ChatQuickAnswer, Long> {

    List<ChatQuickAnswer> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<ChatQuickAnswer> findByTriggerKeyword(String keyword);

    boolean existsByTriggerKeyword(String keyword);
}
