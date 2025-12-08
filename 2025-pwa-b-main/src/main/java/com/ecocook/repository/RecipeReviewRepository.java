package com.ecocook.repository;

import com.ecocook.model.RecipeReview;
import com.ecocook.model.RecipeReview.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeReviewRepository extends JpaRepository<RecipeReview, Long> {
    
    List<RecipeReview> findByRecipeId(Long recipeId);
    
    List<RecipeReview> findByRecipeIdAndStatus(Long recipeId, ReviewStatus status);

    List<RecipeReview> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);
    
    @Query("SELECT r FROM RecipeReview r WHERE r.userName = :username ORDER BY r.createdAt DESC")
    List<RecipeReview> findByUserName(@Param("username") String username);
    
    @Query("SELECT AVG(r.rating) FROM RecipeReview r WHERE r.recipe.id = :recipeId AND r.status = 'APPROVED'")
    Double getAverageRatingByRecipeId(@Param("recipeId") Long recipeId);
    
    @Query("SELECT COUNT(r) FROM RecipeReview r WHERE r.recipe.id = :recipeId AND r.status = 'APPROVED'")
    Long getReviewCountByRecipeId(@Param("recipeId") Long recipeId);
    
    @Query("SELECT r FROM RecipeReview r WHERE r.reported = true ORDER BY r.createdAt DESC")
    List<RecipeReview> findReportedReviews();
    
    @Query("SELECT r FROM RecipeReview r WHERE r.status = 'PENDING' ORDER BY r.createdAt ASC")
    List<RecipeReview> findPendingReviews();
    
    @Query("SELECT r FROM RecipeReview r WHERE r.recipe.id = :recipeId AND r.userName = :username")
    Optional<RecipeReview> findByRecipeIdAndUserName(@Param("recipeId") Long recipeId, @Param("username") String username);
}

