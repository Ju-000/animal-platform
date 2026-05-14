package com.animalplatform.animal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByShelterId(Long shelterId);
}
