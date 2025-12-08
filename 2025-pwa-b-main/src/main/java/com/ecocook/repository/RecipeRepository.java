package com.ecocook.repository;

import com.ecocook.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    
    List<Recipe> findByCategory(String category);
    
    List<Recipe> findByDifficulty(String difficulty);
    
    List<Recipe> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT r FROM Recipe r WHERE r.createdBy = :username")
    List<Recipe> findByCreatedBy(@Param("username") String username);
    
    @Query("SELECT r FROM Recipe r ORDER BY r.name ASC")
    List<Recipe> findAllOrderByName();
}