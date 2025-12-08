package com.ecocook.controller;

import com.ecocook.model.Ingredient;
import com.ecocook.model.Recipe;
import com.ecocook.model.RecipeModification;
import com.ecocook.model.RecipeModification.ModificationStatus;
import com.ecocook.model.IngredientModification;
import com.ecocook.model.RecipeReview;
import com.ecocook.model.RecipeReport;
import com.ecocook.repository.RecipeModificationRepository;
import com.ecocook.repository.RecipeReviewRepository;
import com.ecocook.repository.RecipeReportRepository;
import com.ecocook.service.RecipeSelectionService;
import com.ecocook.service.RecipeService;
import com.ecocook.service.RecipeService.RecipeMatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Gestion des recettes
 */
@Controller
@RequestMapping("/recipes")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;
    
    @Autowired
    private RecipeSelectionService recipeSelectionService;
    
    @Autowired
    private RecipeModificationRepository recipeModificationRepository;
    
    @Autowired
    private RecipeReviewRepository reviewRepository;
    
    @Autowired
    private RecipeReportRepository reportRepository;
    
    @GetMapping
    public String recipesPage(Model model, Authentication authentication) {
        // Nom de l'utilisateur si connecté (utile pour personnaliser les suggestions)
        String userName = authentication != null ? authentication.getName() : null;
        
        // Pour un invité : on renvoie toutes les recettes (score à zéro).
        // Pour un utilisateur : on calcule la correspondance avec son garde-manger.
        List<RecipeMatch> matches = userName != null 
            ? recipeService.findMatchingRecipes(userName) 
            : recipeService.getAllRecipes().stream()
                .map(r -> new RecipeMatch(r, 0, List.of(), r.getIngredients()))
                .toList();
        
        // Répartition simple pour l'affichage par sections dans la vue.
        List<RecipeMatch> fullyAvailable = matches.stream()
            .filter(RecipeMatch::isFullyAvailable)
            .toList();
        
        List<RecipeMatch> partiallyAvailable = matches.stream()
            .filter(RecipeMatch::isPartiallyAvailable)
            .toList();
        
        List<RecipeMatch> notAvailable = matches.stream()
            .filter(m -> !m.isFullyAvailable() && !m.isPartiallyAvailable())
            .toList();
        
        model.addAttribute("fullyAvailable", fullyAvailable);
        model.addAttribute("partiallyAvailable", partiallyAvailable);
        model.addAttribute("notAvailable", notAvailable);
        model.addAttribute("allRecipes", matches);
        
        return "recipes/recipes";
    }
    
    @GetMapping("/{id}")
    public String recipeDetail(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Recipe> recipeOpt = recipeService.getRecipeById(id);
        if (recipeOpt.isEmpty()) {
            return "redirect:/recipes";
        }
        
        Recipe recipe = recipeOpt.get();
        
        // Valeurs par défaut si l'utilisateur consulte sans être connecté
        RecipeMatch currentMatch = null;
        boolean isSelected = false;
        String currentUser = null;
        boolean isAdminUser = false;
        boolean canReplyToReviews = false;
        
        // Pour un membre connecté on calcule plusieurs informations pratiques
        if (authentication != null) {
            String userName = authentication.getName();
            currentUser = userName;
            isAdminUser = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
            List<RecipeMatch> matches = recipeService.findMatchingRecipes(userName);
            currentMatch = matches.stream()
                .filter(m -> m.getRecipe().getId().equals(id))
                .findFirst()
                .orElse(null);
            isSelected = recipeSelectionService.isRecipeSelected(userName, id);
            canReplyToReviews = isAdminUser || (recipe.getCreatedBy() != null && recipe.getCreatedBy().equals(userName));
        }
        
        // Seul l'auteur ou un admin peut agir sur la recette
        boolean canEdit = false;
        if (authentication != null) {
            String userName = authentication.getName();
            canEdit = isAdminUser || recipe.getCreatedBy() != null && recipe.getCreatedBy().equals(userName);
        }
        
        List<RecipeReview> reviews = reviewRepository.findByRecipeIdOrderByCreatedAtDesc(id);
        Double averageRating = reviewRepository.getAverageRatingByRecipeId(id);
        Long reviewCount = reviewRepository.getReviewCountByRecipeId(id);
        
        boolean hasUserReviewed = false;
        boolean hasUserReported = false;
        Map<Long, Boolean> userHasRootReply = new HashMap<>();
        Map<Long, Boolean> userHasChildReply = new HashMap<>();
        if (authentication != null) {
            Optional<RecipeReview> userReview = reviewRepository.findByRecipeIdAndUserName(id, authentication.getName());
            hasUserReviewed = userReview.isPresent();
            Optional<RecipeReport> userReport = reportRepository.findPendingReportByRecipeIdAndUser(id, authentication.getName());
            hasUserReported = userReport.isPresent();

            if (canReplyToReviews) {
                String userName = authentication.getName();
                for (RecipeReview reviewItem : reviews) {
                    boolean hasRootReply = reviewItem.getReplies().stream()
                            .anyMatch(reply -> reply.getParent() == null && userName.equals(reply.getUserName()));
                    userHasRootReply.put(reviewItem.getId(), hasRootReply);

                    reviewItem.getReplies().forEach(reply -> {
                        boolean hasChildReply = reply.getChildren().stream()
                                .anyMatch(child -> userName.equals(child.getUserName()));
                        userHasChildReply.put(reply.getId(), hasChildReply);
                    });
                }
            }
        }
        
        model.addAttribute("recipe", recipe);
        model.addAttribute("match", currentMatch);
        model.addAttribute("isSelected", isSelected);
        model.addAttribute("canEdit", canEdit);
        model.addAttribute("reviews", reviews);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdminUser", isAdminUser);
        model.addAttribute("averageRating", averageRating != null ? averageRating : 0.0);
        model.addAttribute("reviewCount", reviewCount != null ? reviewCount : 0);
        model.addAttribute("hasUserReviewed", hasUserReviewed);
        model.addAttribute("hasUserReported", hasUserReported);
        model.addAttribute("canReplyToReviews", canReplyToReviews);
        model.addAttribute("userHasRootReply", userHasRootReply);
        model.addAttribute("userHasChildReply", userHasChildReply);
        
        return "recipes/recipe-detail";
    }
    
    /**
     * Affiche le formulaire pour créer une nouvelle recette
     * On passe une recette vide au formulaire
     */
    @GetMapping("/new")
    public String newRecipeForm(Model model) {
        model.addAttribute("recipe", new Recipe());
        model.addAttribute("editing", false);
        return "recipes/recipe-form";
    }
    
    @GetMapping("/edit/{id}")
    public String editRecipeForm(@PathVariable Long id, Model model, Authentication authentication) {
        Optional<Recipe> recipe = recipeService.getRecipeById(id);
        if (recipe.isEmpty()) {
            return "redirect:/recipes";
        }
        
        Recipe recipeToEdit = recipe.get();
        String userName = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
        
        // Rappel : seul le créateur ou un admin est autorisé à modifier
        if (!isAdmin && !recipeToEdit.getCreatedBy().equals(userName)) {
            return "redirect:/recipes/" + id;
        }
        
        // Pré-remplissage du formulaire avec les données existantes
        model.addAttribute("recipe", recipeToEdit);
        model.addAttribute("editing", true);
        model.addAttribute("isAdmin", isAdmin);
        return "recipes/recipe-form";
    }
    
    @PostMapping("/save")
    public String saveRecipe(@ModelAttribute Recipe recipe, 
            @RequestParam(required = false) String[] ingredientNames, 
            @RequestParam(required = false) Integer[] ingredientQuantities, 
            @RequestParam(required = false) String[] ingredientUnits,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userName = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
            
            // Création d'une nouvelle recette : on enregistre immédiatement
            if (recipe.getId() == null) {
                Recipe newRecipe = new Recipe();
                newRecipe.setName(recipe.getName());
                newRecipe.setDescription(recipe.getDescription());
                newRecipe.setCategory(recipe.getCategory());
                newRecipe.setDifficulty(recipe.getDifficulty());
                newRecipe.setPreparationTime(recipe.getPreparationTime());
                newRecipe.setCookingTime(recipe.getCookingTime());
                newRecipe.setServings(recipe.getServings());
                newRecipe.setInstructions(recipe.getInstructions());
                newRecipe.setCreatedBy(userName);
                
                if (ingredientNames != null && ingredientNames.length > 0) {
                    for (int i = 0; i < ingredientNames.length; i++) {
                        String name = ingredientNames[i];
                        if (name != null && !name.trim().isEmpty()) {
                            Integer quantity = (ingredientQuantities != null && i < ingredientQuantities.length) 
                                ? ingredientQuantities[i] : null;
                            String unit = (ingredientUnits != null && i < ingredientUnits.length && ingredientUnits[i] != null) 
                                ? ingredientUnits[i].trim() : "";
                            Ingredient ingredient = new Ingredient(name.trim(), quantity, unit);
                            newRecipe.addIngredient(ingredient);
                        }
                    }
                }
                
                recipeService.saveRecipe(newRecipe);
                redirectAttributes.addFlashAttribute("success", "Recette créée avec succès !");
                return "redirect:/recipes";
            }
            
            // À partir d'ici : modification d'une recette existante
            Optional<Recipe> originalRecipeOpt = recipeService.getRecipeById(recipe.getId());
            if (originalRecipeOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Recette non trouvée.");
                return "redirect:/recipes";
            }
            
            Recipe originalRecipe = originalRecipeOpt.get();
            
            // Les administrateurs modifient directement l'original
            if (isAdmin) {
                originalRecipe.setName(recipe.getName());
                originalRecipe.setDescription(recipe.getDescription());
                originalRecipe.setInstructions(recipe.getInstructions());
                originalRecipe.setPreparationTime(recipe.getPreparationTime());
                originalRecipe.setCookingTime(recipe.getCookingTime());
                originalRecipe.setServings(recipe.getServings());
                originalRecipe.setDifficulty(recipe.getDifficulty());
                originalRecipe.setCategory(recipe.getCategory());
                
                originalRecipe.getIngredients().clear();
            if (ingredientNames != null && ingredientNames.length > 0) {
                for (int i = 0; i < ingredientNames.length; i++) {
                    String name = ingredientNames[i];
                    if (name != null && !name.trim().isEmpty()) {
                        Integer quantity = (ingredientQuantities != null && i < ingredientQuantities.length) 
                                    ? ingredientQuantities[i] : null;
                        String unit = (ingredientUnits != null && i < ingredientUnits.length && ingredientUnits[i] != null) 
                                    ? ingredientUnits[i].trim() : "";
                        Ingredient ingredient = new Ingredient(name.trim(), quantity, unit);
                            originalRecipe.addIngredient(ingredient);
                        }
                    }
                }
                
                recipeService.saveRecipe(originalRecipe);
                redirectAttributes.addFlashAttribute("success", "Recette modifiée avec succès !");
                return "redirect:/recipes";
                    }
            
            // Un auteur non admin envoie une proposition de modification
            if (originalRecipe.getCreatedBy().equals(userName)) {
                RecipeModification modification = new RecipeModification();
                modification.setOriginalRecipe(originalRecipe);
                modification.setName(recipe.getName());
                modification.setDescription(recipe.getDescription());
                modification.setInstructions(recipe.getInstructions());
                modification.setPreparationTime(recipe.getPreparationTime());
                modification.setCookingTime(recipe.getCookingTime());
                modification.setServings(recipe.getServings());
                modification.setDifficulty(recipe.getDifficulty());
                modification.setCategory(recipe.getCategory());
                modification.setModifiedBy(userName);
                modification.setStatus(ModificationStatus.PENDING);
            
                if (ingredientNames != null && ingredientNames.length > 0) {
                    for (int i = 0; i < ingredientNames.length; i++) {
                        String name = ingredientNames[i];
                        if (name != null && !name.trim().isEmpty()) {
                            Integer quantity = (ingredientQuantities != null && i < ingredientQuantities.length) 
                                ? ingredientQuantities[i] : null;
                            String unit = (ingredientUnits != null && i < ingredientUnits.length && ingredientUnits[i] != null) 
                                ? ingredientUnits[i].trim() : "";
                            IngredientModification ingMod = new IngredientModification(name.trim(), quantity, unit);
                            modification.addIngredient(ingMod);
                }
            }
                }
                
                recipeModificationRepository.save(modification);
                redirectAttributes.addFlashAttribute("success", "Modification envoyée pour validation. Un administrateur va la vérifier.");
                return "redirect:/recipes";
            }
            
            // Cas restant : l'utilisateur tente de modifier une recette qui ne lui appartient pas
            redirectAttributes.addFlashAttribute("error", "Vous n'avez pas le droit de modifier cette recette.");
            return "redirect:/recipes";
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/recipes";
        }
    }
    
    @PostMapping("/delete/{id}")
    public String deleteRecipe(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            Optional<Recipe> recipeOpt = recipeService.getRecipeById(id);
            if (recipeOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Recette non trouvée.");
                return "redirect:/recipes";
            }
            
            Recipe recipe = recipeOpt.get();
            String userName = authentication.getName();
            boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
            
            // Seul le créateur ou un admin peut supprimer la recette
            if (!isAdmin && !recipe.getCreatedBy().equals(userName)) {
                redirectAttributes.addFlashAttribute("error", "Vous n'avez pas le droit de supprimer cette recette.");
                return "redirect:/recipes";
            }
            
            recipeService.deleteRecipe(id);
            redirectAttributes.addFlashAttribute("success", "Recette supprimée !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression.");
        }
        return "redirect:/recipes";
    }
    
    @PostMapping("/{id}/toggle-selection")
    public String toggleRecipeSelection(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            String userName = authentication.getName();
            boolean wasSelected = recipeSelectionService.isRecipeSelected(userName, id);
            
            recipeSelectionService.toggleRecipeSelection(userName, id);
            
            if (wasSelected) {
                redirectAttributes.addFlashAttribute("success", "Recette retirée de votre plan !");
            } else {
                redirectAttributes.addFlashAttribute("success", "Recette ajoutée à votre plan !");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la sélection.");
        }
        
        return "redirect:/recipes/" + id;
    }
    
    @PostMapping("/{id}/report")
    public String reportRecipe(@PathVariable Long id,
                               @RequestParam(required = false) String reason,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("error", "Vous devez être connecté pour signaler une recette.");
            return "redirect:/recipes/" + id;
        }
        
        Optional<Recipe> recipeOpt = recipeService.getRecipeById(id);
        if (recipeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Recette non trouvée.");
            return "redirect:/recipes";
        }
        
        Recipe recipe = recipeOpt.get();
        String userName = authentication.getName();
        
        // Pas de doublon : un utilisateur ne peut signaler qu'une fois la même recette
        Optional<RecipeReport> existingReport = reportRepository.findPendingReportByRecipeIdAndUser(id, userName);
        if (existingReport.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Vous avez déjà signalé cette recette.");
            return "redirect:/recipes/" + id;
        }
        
        // Création du signalement pour la file d'attente d'un administrateur
        RecipeReport report = new RecipeReport();
        report.setRecipe(recipe);
        report.setReportedBy(userName);
        report.setReportReason(reason != null ? reason : "Recette signalée par l'utilisateur");
        report.setStatus(RecipeReport.ReportStatus.PENDING);
        
        reportRepository.save(report);
        
        redirectAttributes.addFlashAttribute("success", "Recette signalée avec succès. Un administrateur va examiner votre signalement.");
        return "redirect:/recipes/" + id;
    }
}