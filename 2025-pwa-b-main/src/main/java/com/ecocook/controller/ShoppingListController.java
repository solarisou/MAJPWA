package com.ecocook.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecocook.model.RecipeSelection;
import com.ecocook.model.ShoppingListItem;
import com.ecocook.repository.ProductRepository;
import com.ecocook.repository.RecipeRepository;
import com.ecocook.repository.ShoppingListRepository;
import com.ecocook.service.RecipeSelectionService;
import com.ecocook.service.RecipeService;

@Controller
@RequestMapping("/shopping-list")
public class ShoppingListController {

    @Autowired
    private ShoppingListRepository shoppingListRepository;
    
    @Autowired
    private RecipeSelectionService recipeSelectionService;
    
    @Autowired
    private RecipeRepository recipeRepository;
    
    @Autowired
    private RecipeService recipeService;
    
    @Autowired
    private ProductRepository productRepository;
    
    /**
     * Affiche la liste de courses de l'utilisateur
     * On récupère les ingrédients manquants des recettes sélectionnées et les articles manuels
     */
    @GetMapping
    public String shoppingList(Model model, Authentication authentication, CsrfToken csrfToken) {
        String userName = authentication.getName();
        
        // On récupère les recettes que l'utilisateur a sélectionnées pour son plan
        List<RecipeSelection> selections = recipeSelectionService.getUserSelections(userName);
        System.out.println("Nombre de recettes sélectionnées: " + selections.size());
        
        // On calcule quels ingrédients sont disponibles et lesquels manquent
        List<RecipeService.RecipeMatch> allMatches = recipeService.findMatchingRecipes(userName);
        
        // On filtre pour ne garder que les recettes sélectionnées
        List<RecipeService.RecipeMatch> selectedRecipeMatches = selections.stream()
            .map(s -> {
                RecipeService.RecipeMatch match = allMatches.stream()
                    .filter(m -> m.getRecipe().getId().equals(s.getRecipeId()))
                    .findFirst()
                    .orElse(null);
                if (match != null) {
                    System.out.println("Match trouvé pour recette: " + match.getRecipe().getName());
                    System.out.println("  Manquants: " + match.getMissingIngredients().size());
                    System.out.println("  Disponibles: " + match.getMatchedIngredients().size());
                }
                return match;
            })
            .filter(m -> m != null)
            .toList();
        
        // On récupère les articles ajoutés manuellement à la liste
        List<ShoppingListItem> manualItems = shoppingListRepository.findByUserNameOrderByCreatedAtDesc(userName);
        
        // On passe toutes les données au template
        model.addAttribute("selectedRecipeMatches", selectedRecipeMatches);
        model.addAttribute("manualItems", manualItems);
        model.addAttribute("userName", userName);
        
        // On ajoute le token CSRF pour les requêtes AJAX
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        
        return "shopping/shopping-list";
    }
    
    @PostMapping("/add")
    public String addItem(
            @RequestParam String name,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String unit,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            String userName = authentication.getName();
            ShoppingListItem item = new ShoppingListItem(name, quantity, unit, userName);
            shoppingListRepository.save(item);
            redirectAttributes.addFlashAttribute("success", "Article ajouté à la liste !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/shopping-list";
    }
    
    /**
     * Cocher ou décocher un article de la liste
     * Cette méthode est appelée via AJAX, donc on retourne une réponse vide
     * Le JavaScript met à jour l'interface sans recharger la page
     */
    @PostMapping("/toggle")
    @ResponseBody
    public ResponseEntity<Void> toggleItem(
            @RequestParam Long id,
            Authentication authentication) {
        
        try {
            String userName = authentication.getName();
            shoppingListRepository.findById(id).ifPresent(item -> {
                if (item.getUserName().equals(userName)) {
                    item.setChecked(!item.isChecked());
                    shoppingListRepository.save(item);
                }
            });
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Supprimer un article de la liste
     * Cette méthode est appelée via AJAX, donc on retourne une réponse vide
     * Le JavaScript supprime l'élément de la page sans recharger
     */
    @PostMapping("/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteItem(
            @RequestParam Long id,
            Authentication authentication) {
        
        try {
            String userName = authentication.getName();
            shoppingListRepository.findById(id).ifPresent(item -> {
                if (item.getUserName().equals(userName)) {
                    shoppingListRepository.deleteById(id);
                }
            });
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Supprimer tous les articles cochés
     * Cette méthode est appelée via AJAX
     * Le JavaScript va recharger la page après la suppression pour voir les changements
     */
    @PostMapping("/clear-checked")
    @ResponseBody
    public ResponseEntity<Void> clearChecked(Authentication authentication) {
        try {
            String userName = authentication.getName();
            List<ShoppingListItem> checkedItems = shoppingListRepository.findByUserNameAndCheckedOrderByCreatedAtDesc(userName, true);
            checkedItems.forEach(item -> shoppingListRepository.delete(item));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}

