package com.ecocook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecocook.model.RecipeSelection;

@Repository
public interface RecipeSelectionRepository extends JpaRepository<RecipeSelection, Long> {
    
    List<RecipeSelection> findByUserName(String userName);
    
    boolean existsByUserNameAndRecipeId(String userName, Long recipeId);
    
    void deleteByUserNameAndRecipeId(String userName, Long recipeId);
}

