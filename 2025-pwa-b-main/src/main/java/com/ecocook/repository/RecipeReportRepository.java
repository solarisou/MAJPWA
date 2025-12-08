package com.ecocook.repository;

import com.ecocook.model.RecipeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeReportRepository extends JpaRepository<RecipeReport, Long> {
    
    /**
     * Trouve tous les signalements en attente
     */
    @Query("SELECT r FROM RecipeReport r WHERE r.status = 'PENDING' ORDER BY r.createdAt DESC")
    List<RecipeReport> findPendingReports();
    
    /**
     * Trouve tous les signalements pour une recette donnée
     */
    List<RecipeReport> findByRecipeId(Long recipeId);
    
    /**
     * Trouve les signalements en attente pour une recette donnée
     */
    @Query("SELECT r FROM RecipeReport r WHERE r.recipe.id = :recipeId AND r.status = 'PENDING'")
    List<RecipeReport> findPendingReportsByRecipeId(@Param("recipeId") Long recipeId);
    
    /**
     * Vérifie si un utilisateur a déjà signalé une recette
     */
    @Query("SELECT r FROM RecipeReport r WHERE r.recipe.id = :recipeId AND r.reportedBy = :username AND r.status = 'PENDING'")
    Optional<RecipeReport> findPendingReportByRecipeIdAndUser(@Param("recipeId") Long recipeId, @Param("username") String username);
}

