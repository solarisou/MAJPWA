package com.ecocook.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stocke une proposition de modification d'une recette par son auteur.
 * Les administrateurs peuvent ensuite l'approuver ou la rejeter.
 */
@Entity
@Table(name = "recipe_modifications")
public class RecipeModification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe originalRecipe;
    
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(length = 5000)
    private String instructions;
    
    private Integer preparationTime;
    private Integer cookingTime;
    private Integer servings;
    private String difficulty;
    private String category;
    
    @OneToMany(mappedBy = "modification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<IngredientModification> ingredients = new ArrayList<>();
    
    @Column(name = "modified_by", nullable = false)
    private String modifiedBy; // identifiant de l'auteur de la proposition
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ModificationStatus status = ModificationStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "reviewed_by")
    private String reviewedBy; // identifiant de l'administrateur qui tranche
    
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    
    public RecipeModification() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Recipe getOriginalRecipe() { return originalRecipe; }
    public void setOriginalRecipe(Recipe originalRecipe) { this.originalRecipe = originalRecipe; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    
    public Integer getPreparationTime() { return preparationTime; }
    public void setPreparationTime(Integer preparationTime) { this.preparationTime = preparationTime; }
    
    public Integer getCookingTime() { return cookingTime; }
    public void setCookingTime(Integer cookingTime) { this.cookingTime = cookingTime; }
    
    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public List<IngredientModification> getIngredients() { return ingredients; }
    public void setIngredients(List<IngredientModification> ingredients) { this.ingredients = ingredients; }
    
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    
    public ModificationStatus getStatus() { return status; }
    public void setStatus(ModificationStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public void addIngredient(IngredientModification ingredient) {
        // On maintient la relation bidirectionnelle avec l'ingrédient modifié
        ingredients.add(ingredient);
        ingredient.setModification(this);
    }
    
    public void removeIngredient(IngredientModification ingredient) {
        ingredients.remove(ingredient);
        ingredient.setModification(null);
    }
    
    public Integer getTotalTime() {
        return (preparationTime != null ? preparationTime : 0) + (cookingTime != null ? cookingTime : 0);
    }
    
    public enum ModificationStatus {
        PENDING,    // En attente de validation
        APPROVED,   // Approuvée par l'admin
        REJECTED    // Rejetée par l'admin
    }
}

