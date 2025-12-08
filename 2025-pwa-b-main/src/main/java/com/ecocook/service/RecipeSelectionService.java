package com.ecocook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecocook.model.Ingredient;
import com.ecocook.model.Recipe;
import com.ecocook.model.RecipeSelection;
import com.ecocook.repository.RecipeRepository;
import com.ecocook.repository.RecipeSelectionRepository;

@Service
public class RecipeSelectionService {

    @Autowired
    private RecipeSelectionRepository recipeSelectionRepository;
    
    @Autowired
    private RecipeRepository recipeRepository;
    
    @Transactional
    public void toggleRecipeSelection(String userName, Long recipeId) {
        if (recipeSelectionRepository.existsByUserNameAndRecipeId(userName, recipeId)) {
            recipeSelectionRepository.deleteByUserNameAndRecipeId(userName, recipeId);
        } else {
            RecipeSelection selection = new RecipeSelection(userName, recipeId);
            recipeSelectionRepository.save(selection);
        }
    }
    
    public boolean isRecipeSelected(String userName, Long recipeId) {
        return recipeSelectionRepository.existsByUserNameAndRecipeId(userName, recipeId);
    }
    
    public Map<String, Integer> getReservedQuantitiesByProduct(String userName) {
        Map<String, Integer> reserved = new HashMap<>();
        
        List<RecipeSelection> selections = recipeSelectionRepository.findByUserName(userName);
        
        for (RecipeSelection selection : selections) {
            recipeRepository.findById(selection.getRecipeId()).ifPresent(recipe -> {
                for (Ingredient ingredient : recipe.getIngredients()) {
                    String productName = ingredient.getName().toLowerCase();
                    Integer quantity = ingredient.getQuantity() != null ? ingredient.getQuantity() : 1;
                    reserved.put(productName, reserved.getOrDefault(productName, 0) + quantity);
                }
            });
        }
        
        return reserved;
    }
    
    public List<RecipeSelection> getUserSelections(String userName) {
        return recipeSelectionRepository.findByUserName(userName);
    }
}

