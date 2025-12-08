package com.ecocook.controller;

import com.ecocook.model.Recipe;
import com.ecocook.model.RecipeModification;
import com.ecocook.model.RecipeModification.ModificationStatus;
import com.ecocook.model.Ingredient;
import com.ecocook.model.IngredientModification;
import com.ecocook.model.user.User;
import com.ecocook.model.user.UserRepository;
import com.ecocook.model.user.UserRole;
import com.ecocook.model.RecipeReview;
import com.ecocook.model.RecipeReport;
import com.ecocook.repository.ProductCatalogRepository;
import com.ecocook.repository.RecipeModificationRepository;
import com.ecocook.repository.RecipeReviewRepository;
import com.ecocook.repository.RecipeReportRepository;
import com.ecocook.service.RecipeService;
import com.ecocook.secu.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur d'administration
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RecipeModificationRepository recipeModificationRepository;
    
    @Autowired
    private RecipeService recipeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private RecipeReviewRepository reviewRepository;
    
    @Autowired
    private RecipeReportRepository reportRepository;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @GetMapping
    public String adminDashboard(Model model) {
        // Quelques indicateurs simples pour aider l'admin à prioriser son action
        long pendingModifications = recipeModificationRepository.findPendingModifications().size();
        long totalUsers = userRepository.count();
        long reportedReviews = reviewRepository.findReportedReviews().size();
        long reportedRecipes = reportRepository.findPendingReports().size();
        long ingredientsCount = productCatalogRepository.count();
        model.addAttribute("pendingModifications", pendingModifications);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("reportedReviews", reportedReviews);
        model.addAttribute("reportedRecipes", reportedRecipes);
        model.addAttribute("ingredientsCount", ingredientsCount);
        return "admin/dashboard";
    }
    
    @GetMapping("/users/create-admin")
    public String showCreateAdminForm(Model model) {
        model.addAttribute("form", new com.ecocook.formdata.UserRegistrationForm());
        return "admin/create-admin";
    }
    
    @PostMapping("/users/create-admin")
    public String createAdmin(@ModelAttribute com.ecocook.formdata.UserRegistrationForm form,
                            RedirectAttributes redirectAttributes) {
        // Vérifications basiques côté formulaire avant de créer le compte
        if (form.getUserName() == null || form.getUserName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "L'email est obligatoire.");
            return "redirect:/admin/users/create-admin";
        }
        
        if (form.getPassword() == null || form.getPassword().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Le mot de passe doit contenir au moins 6 caractères.");
            return "redirect:/admin/users/create-admin";
        }
        
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/admin/users/create-admin";
        }
        
        // On refuse la création si l'identifiant est déjà utilisé
        if (userRepository.existsById(form.getUserName())) {
            redirectAttributes.addFlashAttribute("error", "Cet utilisateur existe déjà.");
            return "redirect:/admin/users/create-admin";
        }
        
        // Création de l'utilisateur avec les deux rôles nécessaires (ADMIN + USER)
        User admin = new User();
        admin.setUserName(form.getUserName());
        admin.setDisplayName(form.getDisplayName() != null ? form.getDisplayName() : form.getUserName());
        admin.getRoles().add(UserRole.ADMIN);
        admin.getRoles().add(UserRole.USER);
        
        userService.saveUserComputingDerivedPassword(admin, form.getPassword());
        
        redirectAttributes.addFlashAttribute("success", "Compte administrateur créé avec succès !");
        return "redirect:/admin/users";
    }
    
    @GetMapping("/users")
    public String listUsers(Model model) {
        Iterable<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users";
    }
    
    @PostMapping("/users/delete/{username}")
    public String deleteUser(@PathVariable String username, 
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        // Empêcher un admin de se supprimer lui-même : c'est le seul vrai risque ici
        if (username.equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer votre propre compte.");
            return "redirect:/admin/users";
        }
        
        Optional<User> userOpt = userRepository.findById(username);
        if (userOpt.isPresent()) {
            userRepository.delete(userOpt.get());
            redirectAttributes.addFlashAttribute("success", "Utilisateur supprimé avec succès.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé.");
        }
        
        return "redirect:/admin/users";
    }
    
    @GetMapping("/modifications")
    public String listModifications(Model model) {
        List<RecipeModification> pendingModifications = recipeModificationRepository.findPendingModifications();
        model.addAttribute("modifications", pendingModifications);
        return "admin/modifications";
    }
    
    @GetMapping("/modifications/{id}")
    public String viewModification(@PathVariable Long id, Model model) {
        Optional<RecipeModification> modificationOpt = recipeModificationRepository.findById(id);
        if (modificationOpt.isEmpty()) {
            return "redirect:/admin/modifications";
        }
        
        RecipeModification modification = modificationOpt.get();
        model.addAttribute("modification", modification);
        model.addAttribute("originalRecipe", modification.getOriginalRecipe());
        return "admin/modification-detail";
    }
    
    @PostMapping("/modifications/{id}/approve")
    public String approveModification(@PathVariable Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        Optional<RecipeModification> modificationOpt = recipeModificationRepository.findById(id);
        if (modificationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Modification non trouvée.");
            return "redirect:/admin/modifications";
        }
        
        RecipeModification modification = modificationOpt.get();
        
        // Appliquer les changements validés sur l'entité de référence
        Recipe originalRecipe = modification.getOriginalRecipe();
        originalRecipe.setName(modification.getName());
        originalRecipe.setDescription(modification.getDescription());
        originalRecipe.setInstructions(modification.getInstructions());
        originalRecipe.setPreparationTime(modification.getPreparationTime());
        originalRecipe.setCookingTime(modification.getCookingTime());
        originalRecipe.setServings(modification.getServings());
        originalRecipe.setDifficulty(modification.getDifficulty());
        originalRecipe.setCategory(modification.getCategory());
        
        // Harmoniser la liste d'ingrédients en repartant de zéro
        originalRecipe.getIngredients().clear();
        for (IngredientModification ingMod : modification.getIngredients()) {
            Ingredient ingredient = new Ingredient(ingMod.getName(), ingMod.getQuantity(), ingMod.getUnit());
            originalRecipe.addIngredient(ingredient);
        }
        
        // Sauvegarde de la recette mise à jour
        recipeService.saveRecipe(originalRecipe);
        
        // Historiser la décision pour suivre le travail de modération
        modification.setStatus(ModificationStatus.APPROVED);
        modification.setReviewedAt(LocalDateTime.now());
        modification.setReviewedBy(authentication.getName());
        recipeModificationRepository.save(modification);
        
        redirectAttributes.addFlashAttribute("success", "Modification approuvée et appliquée à la recette.");
        return "redirect:/admin/modifications";
    }
    
    @PostMapping("/modifications/{id}/reject")
    public String rejectModification(@PathVariable Long id,
                                   @RequestParam(required = false) String reason,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        Optional<RecipeModification> modificationOpt = recipeModificationRepository.findById(id);
        if (modificationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Modification non trouvée.");
            return "redirect:/admin/modifications";
        }
        
        RecipeModification modification = modificationOpt.get();
        modification.setStatus(ModificationStatus.REJECTED);
        modification.setReviewedAt(LocalDateTime.now());
        modification.setReviewedBy(authentication.getName());
        modification.setRejectionReason(reason != null ? reason : "Modification rejetée par l'administrateur.");
        recipeModificationRepository.save(modification);
        
        redirectAttributes.addFlashAttribute("success", "Modification rejetée.");
        return "redirect:/admin/modifications";
    }
    
    @GetMapping("/reviews/reported")
    public String listReportedReviews(Model model) {
        List<RecipeReview> reportedReviews = reviewRepository.findReportedReviews();
        model.addAttribute("reviews", reportedReviews);
        return "admin/reported-reviews";
    }
    
    @PostMapping("/reviews/{id}/approve")
    public String approveReview(@PathVariable Long id,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        Optional<RecipeReview> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Avis non trouvé.");
            return "redirect:/admin/reviews/reported";
        }
        
        RecipeReview review = reviewOpt.get();
        review.setStatus(RecipeReview.ReviewStatus.APPROVED);
        review.setReported(false);
        review.setReviewedAt(LocalDateTime.now());
        review.setReviewedBy(authentication.getName());
        reviewRepository.save(review);
        
        redirectAttributes.addFlashAttribute("success", "Avis approuvé.");
        return "redirect:/admin/reviews/reported";
    }
    
    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        Optional<RecipeReview> reviewOpt = reviewRepository.findById(id);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Avis non trouvé.");
            return "redirect:/admin/reviews/reported";
        }
        
        RecipeReview review = reviewOpt.get();
        reviewRepository.delete(review);
        
        redirectAttributes.addFlashAttribute("success", "Avis supprimé.");
        return "redirect:/admin/reviews/reported";
    }
    
    @GetMapping("/recipes/reported")
    public String listReportedRecipes(Model model) {
        List<RecipeReport> reportedRecipes = reportRepository.findPendingReports();
        model.addAttribute("reports", reportedRecipes);
        return "admin/reported-recipes";
    }
    
    @GetMapping("/recipes/reports/{id}")
    public String viewRecipeReport(@PathVariable Long id, Model model) {
        Optional<RecipeReport> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            return "redirect:/admin/recipes/reported";
        }
        
        RecipeReport report = reportOpt.get();
        model.addAttribute("report", report);
        model.addAttribute("recipe", report.getRecipe());
        return "admin/recipe-report-detail";
    }
    
    @PostMapping("/recipes/reports/{id}/delete-recipe")
    public String deleteReportedRecipe(@PathVariable Long id,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        Optional<RecipeReport> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Signalement non trouvé.");
            return "redirect:/admin/recipes/reported";
        }
        
        RecipeReport report = reportOpt.get();
        Recipe recipe = report.getRecipe();
        
        // Dans ce scénario on supprime complètement la recette signalée
        recipeService.deleteRecipe(recipe.getId());
        
        // On clôture ensuite le signalement avec un statut explicite
        report.setStatus(RecipeReport.ReportStatus.RESOLVED);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(authentication.getName());
        report.setReviewNote("Recette supprimée par l'administrateur");
        reportRepository.save(report);
        
        redirectAttributes.addFlashAttribute("success", "Recette supprimée avec succès.");
        return "redirect:/admin/recipes/reported";
    }
    
    @PostMapping("/recipes/reports/{id}/dismiss")
    public String dismissRecipeReport(@PathVariable Long id,
                                     @RequestParam(required = false) String note,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        Optional<RecipeReport> reportOpt = reportRepository.findById(id);
        if (reportOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Signalement non trouvé.");
            return "redirect:/admin/recipes/reported";
        }
        
        RecipeReport report = reportOpt.get();
        report.setStatus(RecipeReport.ReportStatus.DISMISSED);
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(authentication.getName());
        report.setReviewNote(note != null ? note : "Signalement rejeté - La recette est valide");
        reportRepository.save(report);
        
        redirectAttributes.addFlashAttribute("success", "Signalement rejeté. La recette reste disponible.");
        return "redirect:/admin/recipes/reported";
    }
}

