package com.ecocook.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecocook.model.Product;
import com.ecocook.repository.ProductRepository;
import com.ecocook.service.RecipeSelectionService;

@Controller
@RequestMapping("/pantry")
public class PantryController {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private RecipeSelectionService recipeSelectionService;

    @GetMapping
    public String pantry(Model model, Authentication authentication) {
        String userName = authentication.getName();
        List<Product> products = productRepository.findByUserNameOrderByExpiryDateAsc(userName);
        
        // Grouper les produits par type de stockage
        List<Product> frigo = products.stream().filter(p -> "frigo".equals(p.getStorageType())).toList();
        List<Product> congelateur = products.stream().filter(p -> "congelateur".equals(p.getStorageType())).toList();
        List<Product> placard = products.stream().filter(p -> "placard".equals(p.getStorageType())).toList();
        List<Product> panier = products.stream().filter(p -> "panier".equals(p.getStorageType())).toList();
        
        Map<String, Integer> reserved = recipeSelectionService.getReservedQuantitiesByProduct(userName);
        
        model.addAttribute("products", products);
        model.addAttribute("frigo", frigo);
        model.addAttribute("congelateur", congelateur);
        model.addAttribute("placard", placard);
        model.addAttribute("panier", panier);
        model.addAttribute("reserved", reserved);
        model.addAttribute("userName", userName);
        return "pantry/pantry";
    }
    
    @PostMapping("/add")
    public String addProduct(@RequestParam String name, 
                            @RequestParam int quantity,
                            @RequestParam(required = false) String unit,
                            @RequestParam String expiryDate,
                            @RequestParam(defaultValue = "placard") String storageType,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String userName = authentication.getName();
            Product product = new Product(name, quantity, unit, LocalDate.parse(expiryDate), storageType, userName);
            productRepository.save(product);
            redirectAttributes.addFlashAttribute("success", "Produit ajouté avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'ajout : " + e.getMessage());
        }
        return "redirect:/pantry";
    }
    
    @PostMapping("/delete")
    public String deleteProduct(@RequestParam Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userName = authentication.getName();
            productRepository.findById(id).ifPresent(product -> {
                if (product.getUserName().equals(userName)) {
                    productRepository.deleteById(id);
                    redirectAttributes.addFlashAttribute("success", "Produit supprimé !");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Vous ne pouvez pas supprimer ce produit.");
                }
            });
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la suppression.");
        }
        return "redirect:/pantry";
    }
}