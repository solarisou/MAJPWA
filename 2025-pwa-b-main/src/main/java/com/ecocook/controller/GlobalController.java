package com.ecocook.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecocook.formdata.UserRegistrationForm;
import com.ecocook.model.user.User;
import com.ecocook.model.user.UserRepository;
import com.ecocook.secu.UserService;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;

import com.ecocook.repository.ShoppingListRepository;
import com.ecocook.repository.ProductRepository;
import com.ecocook.repository.RecipeRepository;

/**
 * Contrôleur des pages publiques : accueil, connexion, inscription et profil.
 * On centralise ici les écrans qui ne dépendent pas d'un domaine métier précis.
 */
@Controller
public class GlobalController {

    @Inject
    private UserService userService;
    
    @Inject
    private UserRepository userRepository;

    @Inject
    private ProductRepository productRepository;

    @Inject
    private RecipeRepository recipeRepository;

    @Inject 
    private ShoppingListRepository shoppingListRepository;



    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String home() {
        return "public/home";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        return "public/login";
    }
    

    /**
     * Affiche la page d'inscription avec un formulaire vierge.
     */
    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String showRegistrationForm(Model model) {
        model.addAttribute("form", new UserRegistrationForm());
        return "public/register";
    }
    
    /**
     * Affiche le profil courant, en ajoutant quelques informations pratiques au modèle.
     */
    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public String profile(Model model, Authentication authentication) {
        if (authentication != null) {
            String userName = authentication.getName();
            java.util.Optional<User> userOpt = userRepository.findById(userName);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                model.addAttribute("user", user);
                model.addAttribute("displayName", user.getDisplayName());
                // Vérifier si l'utilisateur est admin
                boolean isAdmin = user.getRoles().contains(com.ecocook.model.user.UserRole.ADMIN);
                model.addAttribute("isAdmin", isAdmin);

                // Calculer les statistiques pour l'utilisateur
                
                // 1. Nombre de produits en stock
                long productsCount = productRepository.findByUserNameOrderByExpiryDateAsc(userName).size();
                model.addAttribute("productsCount", productsCount);
                
                // 2. Nombre de recettes créées par l'utilisateur
                long recipesCount = recipeRepository.findByCreatedBy(userName).size();
                model.addAttribute("recipesCount", recipesCount);
                
                // 3. Nombre d'articles dans la liste de courses (non cochés)
                long shoppingListCount = shoppingListRepository.findByUserNameAndCheckedOrderByCreatedAtDesc(userName, false).size();
                model.addAttribute("shoppingListCount", shoppingListCount);
            }
        }
        return "user/profile";
    }
    
    /**
     * Traite la modification de mot de passe côté profil utilisateur.
     */
    @RequestMapping(value = "/profile/change-password", method = RequestMethod.POST)
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        // Premier garde-fou : les deux saisies doivent correspondre
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Les mots de passe ne correspondent pas.");
            return "redirect:/profile";
        }
        
        // Deuxième garde-fou : mot de passe minimalement robuste
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Le mot de passe doit contenir au moins 6 caractères.");
            return "redirect:/profile";
        }
        
        redirectAttributes.addFlashAttribute("success", "Mot de passe modifié avec succès !");
        return "redirect:/profile";
    }

    /**
     * Enregistre un nouvel utilisateur après validation du formulaire.
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerUser(@Valid @ModelAttribute("form") UserRegistrationForm form,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        
        // Vérifier en priorité la validité du formulaire transmis à Spring
        if (result.hasErrors()) {
            return "public/register";
        }
        
        // Ensuite on s'assure que l'identifiant n'est pas déjà pris
        if (userService.userExists(form.getUserName())) {
            result.rejectValue("userName", "error.exists", "Cet email existe déjà");
            return "public/register";
        }
        
        // Vérifier la cohérence des mots de passe saisis côté utilisateur
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.mismatch", "Les mots de passe ne correspondent pas");
            return "public/register";
        }
        
        // Création de l'entité utilisateur avec les informations principales
        User newUser = new User(form.getUserName());
        newUser.setDisplayName(form.getDisplayName());
        userService.saveUserComputingDerivedPassword(newUser, form.getPassword());
        
        // Message de confirmation affiché sur la page de connexion
        redirectAttributes.addFlashAttribute("success", "Compte créé !");
        return "redirect:/login";
    }

}

