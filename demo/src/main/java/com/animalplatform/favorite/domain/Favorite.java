package com.animalplatform.favorite.domain;

import com.animalplatform.animal.domain.Animal;
import com.animalplatform.common.entity.BaseTimeEntity;
import com.animalplatform.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "favorites")
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    protected Favorite() {
    }

    public Favorite(User user, Animal animal) {
        this.user = user;
        this.animal = animal;
    }

    public Long getId() {
        return id;
    }
}
