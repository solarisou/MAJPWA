package com.ecocook.service;

import com.ecocook.model.Ingredient;
import com.ecocook.model.Product;
import com.ecocook.model.Recipe;
import com.ecocook.repository.ProductRepository;
import com.ecocook.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    @Autowired
    private RecipeRepository recipeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<Recipe> getAllRecipes() {
        return recipeRepository.findAllOrderByName();
    }
    
    public Optional<Recipe> getRecipeById(Long id) {
        return recipeRepository.findById(id);
    }
    
    /**
     * Sauvegarde une recette avec tous ses ingrédients dans la base de données
     * Cette méthode s'assure que tous les ingrédients sont bien liés à la recette
     * avant de les sauvegarder
     */
    @Transactional
    public Recipe saveRecipe(Recipe recipe) {
        // On vérifie que chaque ingrédient sait à quelle recette il appartient
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getRecipe() == null) {
                    ingredient.setRecipe(recipe);
                }
            }
        }
        // On sauvegarde la recette dans la base de donnée
        // Les ingrédients seront automatiquement sauvegardés aussi grâce à la configuration cascade
        // saveAndFlush écrit immédiatement les données (pas besoin d'attendre la fin de la transaction)
        Recipe savedRecipe = recipeRepository.saveAndFlush(recipe);
        return savedRecipe;
    }
    
    @Transactional
    public void deleteRecipe(Long id) {
        recipeRepository.deleteById(id);
    }
    
    public List<RecipeMatch> findMatchingRecipes(String userName) {
        List<Product> userProducts = productRepository.findByUserNameOrderByExpiryDateAsc(userName);
        Set<String> availableProducts = userProducts.stream()
            .map(p -> p.getName().toLowerCase().trim())
            .collect(Collectors.toSet());
        
        List<Recipe> allRecipes = recipeRepository.findAll();
        List<RecipeMatch> matches = new ArrayList<>();
        
        for (Recipe recipe : allRecipes) {
            RecipeMatch match = calculateMatch(recipe, availableProducts, userProducts);
            matches.add(match);
        }
        
        matches.sort((m1, m2) -> Double.compare(m2.getMatchPercentage(), m1.getMatchPercentage()));
        
        return matches;
    }
    
    private RecipeMatch calculateMatch(Recipe recipe, Set<String> availableProducts, List<Product> userProducts) {
        List<Ingredient> ingredients = recipe.getIngredients();
        
        if (ingredients.isEmpty()) {
            return new RecipeMatch(recipe, 0.0, new ArrayList<>(), new ArrayList<>());
        }
        
        List<Ingredient> matchedIngredients = new ArrayList<>();
        List<Ingredient> missingIngredients = new ArrayList<>();
        
        for (Ingredient ingredient : ingredients) {
            String ingredientName = ingredient.getName().toLowerCase().trim();
            boolean found = availableProducts.stream()
                .anyMatch(product -> product.contains(ingredientName) || ingredientName.contains(product));
            
            if (found) {
                matchedIngredients.add(ingredient);
            } else {
                missingIngredients.add(ingredient);
            }
        }
        
        double matchPercentage = (double) matchedIngredients.size() / ingredients.size() * 100;
        
        return new RecipeMatch(recipe, matchPercentage, matchedIngredients, missingIngredients);
    }
    
    public static class RecipeMatch {
        private Recipe recipe;
        private double matchPercentage;
        private List<Ingredient> matchedIngredients;
        private List<Ingredient> missingIngredients;
        
        public RecipeMatch(Recipe recipe, double matchPercentage, List<Ingredient> matchedIngredients, List<Ingredient> missingIngredients) {
            this.recipe = recipe;
            this.matchPercentage = matchPercentage;
            this.matchedIngredients = matchedIngredients;
            this.missingIngredients = missingIngredients;
        }
        
        public Recipe getRecipe() { return recipe; }
        public double getMatchPercentage() { return matchPercentage; }
        public List<Ingredient> getMatchedIngredients() { return matchedIngredients; }
        public List<Ingredient> getMissingIngredients() { return missingIngredients; }
        
        public boolean isFullyAvailable() { return matchPercentage == 100.0; }
        public boolean isPartiallyAvailable() { return matchPercentage >= 50.0 && matchPercentage < 100.0; }
    }
}