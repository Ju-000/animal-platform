package com.animalplatform.favorite.application;

import com.animalplatform.favorite.domain.UserFavorite;
import com.animalplatform.favorite.domain.UserFavoriteRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class FavoriteService {

    private final UserFavoriteRepository userFavoriteRepository;

    public FavoriteService(UserFavoriteRepository userFavoriteRepository) {
        this.userFavoriteRepository = userFavoriteRepository;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, String animalNo) {
        String normalizedAnimalNo = normalizeAnimalNo(animalNo);
        if (userFavoriteRepository.existsByUserIdAndAnimalNo(userId, normalizedAnimalNo)) {
            userFavoriteRepository.deleteByUserIdAndAnimalNo(userId, normalizedAnimalNo);
            return false;
        }

        userFavoriteRepository.save(new UserFavorite(userId, normalizedAnimalNo));
        return true;
    }

    @Transactional
    public boolean addFavorite(Long userId, String animalNo) {
        String normalizedAnimalNo = normalizeAnimalNo(animalNo);
        if (!userFavoriteRepository.existsByUserIdAndAnimalNo(userId, normalizedAnimalNo)) {
            userFavoriteRepository.save(new UserFavorite(userId, normalizedAnimalNo));
        }
        return true;
    }

    @Transactional
    public boolean removeFavorite(Long userId, String animalNo) {
        String normalizedAnimalNo = normalizeAnimalNo(animalNo);
        if (!userFavoriteRepository.existsByUserIdAndAnimalNo(userId, normalizedAnimalNo)
                && userFavoriteRepository.existsByAnimalNo(normalizedAnimalNo)) {
            throw new ResponseStatusException(FORBIDDEN, "Cannot delete another user's favorite.");
        }
        userFavoriteRepository.deleteByUserIdAndAnimalNo(userId, normalizedAnimalNo);
        return false;
    }

    @Transactional(readOnly = true)
    public List<String> getFavoriteAnimalNos(Long userId) {
        return userFavoriteRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(UserFavorite::getSavedAt).reversed())
                .map(UserFavorite::getAnimalNo)
                .toList();
    }

    private String normalizeAnimalNo(String animalNo) {
        String normalized = String.valueOf(animalNo).trim();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "동물 식별자는 필수입니다.");
        }
        return normalized;
    }
}
