package com.ssafy.manager.nutrition.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodJpaRepository extends JpaRepository<FoodEntity, String> {

    @Query("SELECT f FROM FoodEntity f WHERE f.name LIKE %:keyword%")
    List<FoodEntity> findByNameContaining(@Param("keyword") String keyword);
}
