package com.ecocook.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.ecocook.model.RecipeSelection;
import com.ecocook.model.ShoppingListItem;
import com.ecocook.model.Recipe;
import com.ecocook.model.user.User;
import com.ecocook.model.user.UserRepository;
import com.ecocook.repository.ProductRepository;
import com.ecocook.repository.RecipeRepository;
import com.ecocook.repository.ShoppingListRepository;
import com.ecocook.service.RecipeSelectionService;
import com.ecocook.service.RecipeService;

/**
 * API REST pour l'application Vue.js PWA
 */
@RestController
@RequestMapping("/api/vue")
public class VueApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private RecipeSelectionService recipeSelectionService;

    @Autowired
    private RecipeService recipeService;

    // ==================== Utilisateur ====================

    /**
     * GET /api/vue/user - Informations de l'utilisateur connecte
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();
        Optional<User> userOpt = userRepository.findById(userName);

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("userName", user.getUserName());
        response.put("displayName", user.getDisplayName());
        response.put("isAdmin", user.getRoles().contains(com.ecocook.model.user.UserRole.ADMIN));

        // Statistiques
        long productsCount = productRepository.findByUserNameOrderByExpiryDateAsc(userName).size();
        long recipesCount = recipeSelectionService.getUserSelections(userName).size();
        long shoppingListCount = shoppingListRepository.findByUserNameAndCheckedOrderByCreatedAtDesc(userName, false).size();

        response.put("productsCount", productsCount);
        response.put("selectedRecipesCount", recipesCount);
        response.put("shoppingListCount", shoppingListCount);

        return ResponseEntity.ok(response);
    }

    // ==================== Recettes selectionnees ====================

    /**
     * GET /api/vue/recipes/selected - Liste des recettes selectionnees par l'utilisateur
     */
    @GetMapping("/recipes/selected")
    public ResponseEntity<List<Map<String, Object>>> getSelectedRecipes(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();
        List<RecipeSelection> selections = recipeSelectionService.getUserSelections(userName);
        List<RecipeService.RecipeMatch> allMatches = recipeService.findMatchingRecipes(userName);

        List<Map<String, Object>> result = selections.stream()
            .map(selection -> {
                Map<String, Object> item = new HashMap<>();
                item.put("selectionId", selection.getId());
                item.put("recipeId", selection.getRecipeId());
                item.put("selectedAt", selection.getSelectedAt());

                // Trouver la recette correspondante
                Optional<Recipe> recipeOpt = recipeRepository.findById(selection.getRecipeId());
                if (recipeOpt.isPresent()) {
                    Recipe recipe = recipeOpt.get();
                    item.put("name", recipe.getName());
                    item.put("description", recipe.getDescription());
                    item.put("instructions", recipe.getInstructions());
                    item.put("category", recipe.getCategory());
                    item.put("difficulty", recipe.getDifficulty());
                    item.put("preparationTime", recipe.getPreparationTime());
                    item.put("cookingTime", recipe.getCookingTime());
                    item.put("servings", recipe.getServings());

                    // Liste complete des ingredients
                    item.put("ingredients", recipe.getIngredients().stream()
                        .map(ing -> Map.of(
                            "name", ing.getName(),
                            "quantity", ing.getQuantity() != null ? ing.getQuantity() : 0,
                            "unit", ing.getUnit() != null ? ing.getUnit() : ""
                        ))
                        .collect(Collectors.toList()));

                    // Trouver le match pour les ingredients disponibles/manquants
                    RecipeService.RecipeMatch match = allMatches.stream()
                        .filter(m -> m.getRecipe().getId().equals(selection.getRecipeId()))
                        .findFirst()
                        .orElse(null);

                    if (match != null) {
                        item.put("matchPercentage", match.getMatchPercentage());
                        item.put("matchedIngredients", match.getMatchedIngredients().stream()
                            .map(ing -> Map.of("name", ing.getName(), "quantity", ing.getQuantity(), "unit", ing.getUnit() != null ? ing.getUnit() : ""))
                            .collect(Collectors.toList()));
                        item.put("missingIngredients", match.getMissingIngredients().stream()
                            .map(ing -> Map.of("name", ing.getName(), "quantity", ing.getQuantity(), "unit", ing.getUnit() != null ? ing.getUnit() : ""))
                            .collect(Collectors.toList()));
                    }
                }

                return item;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/vue/recipes/selected/{recipeId} - Retirer une recette de la selection
     */
    @DeleteMapping("/recipes/selected/{recipeId}")
    public ResponseEntity<Map<String, String>> removeSelectedRecipe(@PathVariable Long recipeId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();

        if (recipeSelectionService.isRecipeSelected(userName, recipeId)) {
            recipeSelectionService.toggleRecipeSelection(userName, recipeId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Recette retiree de la selection");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    // ==================== Liste de courses ====================

    /**
     * GET /api/vue/shopping-list - Liste de courses de l'utilisateur
     */
    @GetMapping("/shopping-list")
    public ResponseEntity<Map<String, Object>> getShoppingList(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();

        // Articles manuels
        List<ShoppingListItem> manualItems = shoppingListRepository.findByUserNameOrderByCreatedAtDesc(userName);

        // Ingredients manquants des recettes selectionnees
        List<RecipeSelection> selections = recipeSelectionService.getUserSelections(userName);
        List<RecipeService.RecipeMatch> allMatches = recipeService.findMatchingRecipes(userName);

        List<Map<String, Object>> recipeIngredients = selections.stream()
            .flatMap(selection -> {
                RecipeService.RecipeMatch match = allMatches.stream()
                    .filter(m -> m.getRecipe().getId().equals(selection.getRecipeId()))
                    .findFirst()
                    .orElse(null);

                if (match != null) {
                    return match.getMissingIngredients().stream()
                        .map(ing -> {
                            Map<String, Object> item = new HashMap<>();
                            item.put("name", ing.getName());
                            item.put("quantity", ing.getQuantity());
                            item.put("unit", ing.getUnit() != null ? ing.getUnit() : "");
                            item.put("fromRecipe", match.getRecipe().getName());
                            item.put("recipeId", match.getRecipe().getId());
                            return item;
                        });
                }
                return java.util.stream.Stream.empty();
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("manualItems", manualItems.stream()
            .map(item -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", item.getId());
                map.put("name", item.getName());
                map.put("quantity", item.getQuantity());
                map.put("unit", item.getUnit());
                map.put("checked", item.isChecked());
                map.put("createdAt", item.getCreatedAt());
                return map;
            })
            .collect(Collectors.toList()));
        response.put("recipeIngredients", recipeIngredients);
        response.put("totalManual", manualItems.size());
        response.put("totalFromRecipes", recipeIngredients.size());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/vue/shopping-list - Ajouter un article a la liste
     */
    @PostMapping("/shopping-list")
    public ResponseEntity<Map<String, Object>> addShoppingItem(@RequestBody Map<String, Object> payload, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String userName = authentication.getName();
            String name = (String) payload.get("name");
            Integer quantity = payload.get("quantity") != null
                ? (payload.get("quantity") instanceof Integer ? (Integer) payload.get("quantity") : Integer.parseInt(payload.get("quantity").toString()))
                : null;
            String unit = (String) payload.get("unit");

            ShoppingListItem item = new ShoppingListItem(name, quantity, unit, userName);
            ShoppingListItem saved = shoppingListRepository.save(item);

            Map<String, Object> response = new HashMap<>();
            response.put("id", saved.getId());
            response.put("name", saved.getName());
            response.put("quantity", saved.getQuantity());
            response.put("unit", saved.getUnit());
            response.put("checked", saved.isChecked());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/vue/shopping-list/{id}/toggle - Cocher/decocher un article
     */
    @PutMapping("/shopping-list/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleShoppingItem(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();

        return shoppingListRepository.findById(id)
            .filter(item -> item.getUserName().equals(userName))
            .map(item -> {
                item.setChecked(!item.isChecked());
                ShoppingListItem saved = shoppingListRepository.save(item);
                Map<String, Object> response = new HashMap<>();
                response.put("id", saved.getId());
                response.put("checked", saved.isChecked());
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/vue/shopping-list/{id} - Supprimer un article
     */
    @DeleteMapping("/shopping-list/{id}")
    public ResponseEntity<Map<String, String>> deleteShoppingItem(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();

        return shoppingListRepository.findById(id)
            .filter(item -> item.getUserName().equals(userName))
            .map(item -> {
                shoppingListRepository.deleteById(id);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Article supprime");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/vue/shopping-list/checked - Supprimer tous les articles coches
     */
    @DeleteMapping("/shopping-list/checked")
    public ResponseEntity<Map<String, String>> clearCheckedItems(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String userName = authentication.getName();
        List<ShoppingListItem> checkedItems = shoppingListRepository.findByUserNameAndCheckedOrderByCreatedAtDesc(userName, true);
        checkedItems.forEach(item -> shoppingListRepository.delete(item));

        Map<String, String> response = new HashMap<>();
        response.put("message", checkedItems.size() + " articles supprimes");
        return ResponseEntity.ok(response);
    }
}
