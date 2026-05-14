package com.animalplatform.story.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdoptionStoryRepository extends JpaRepository<AdoptionStory, Long> {

    Page<AdoptionStory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
