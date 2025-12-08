package com.ecocook.model;

import jakarta.persistence.*;

/**
 * Ingrédient proposé dans le cadre d'une modification de recette.
 */
@Entity
@Table(name = "ingredient_modifications")
public class IngredientModification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "modification_id", nullable = false)
    private RecipeModification modification;
    
    private String name;
    private Integer quantity;
    private String unit;
    
    public IngredientModification() {}
    
    public IngredientModification(String name, Integer quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public RecipeModification getModification() { return modification; }
    public void setModification(RecipeModification modification) { this.modification = modification; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}

