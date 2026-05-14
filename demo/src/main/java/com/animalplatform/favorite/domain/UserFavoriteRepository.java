package com.animalplatform.favorite.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, Long> {

    List<UserFavorite> findAllByUserId(Long userId);

    List<UserFavorite> findAllByAnimalNoIn(List<String> animalNos);

    boolean existsByUserIdAndAnimalNo(Long userId, String animalNo);

    boolean existsByAnimalNo(String animalNo);

    void deleteByUserIdAndAnimalNo(Long userId, String animalNo);
}
