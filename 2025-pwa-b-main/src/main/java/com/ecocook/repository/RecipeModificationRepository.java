package com.ecocook.repository;

import com.ecocook.model.RecipeModification;
import com.ecocook.model.RecipeModification.ModificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeModificationRepository extends JpaRepository<RecipeModification, Long> {
    
    List<RecipeModification> findByStatus(ModificationStatus status);
    
    @Query("SELECT rm FROM RecipeModification rm WHERE rm.modifiedBy = :username ORDER BY rm.createdAt DESC")
    List<RecipeModification> findByModifiedBy(@Param("username") String username);
    
    @Query("SELECT rm FROM RecipeModification rm WHERE rm.originalRecipe.id = :recipeId AND rm.status = :status")
    List<RecipeModification> findByOriginalRecipeIdAndStatus(@Param("recipeId") Long recipeId, @Param("status") ModificationStatus status);
    
    @Query("SELECT rm FROM RecipeModification rm WHERE rm.status = 'PENDING' ORDER BY rm.createdAt ASC")
    List<RecipeModification> findPendingModifications();
}

