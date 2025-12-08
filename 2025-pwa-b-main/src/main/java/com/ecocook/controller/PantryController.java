package com.ecocook.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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

    // ==================== API REST pour Vue.js ====================

    // GET /pantry/api/products - Récupérer tous les produits de l'utilisateur
    @GetMapping("/api/products")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProducts(Authentication authentication) {
        String userName = authentication.getName();
        List<Product> products = productRepository.findByUserNameOrderByExpiryDateAsc(userName);

        List<Product> frigo = products.stream().filter(p -> "frigo".equals(p.getStorageType())).toList();
        List<Product> congelateur = products.stream().filter(p -> "congelateur".equals(p.getStorageType())).toList();
        List<Product> placard = products.stream().filter(p -> "placard".equals(p.getStorageType())).toList();
        List<Product> panier = products.stream().filter(p -> "panier".equals(p.getStorageType())).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("frigo", frigo);
        response.put("congelateur", congelateur);
        response.put("placard", placard);
        response.put("panier", panier);
        response.put("total", products.size());

        return ResponseEntity.ok(response);
    }

    // GET /pantry/api/products/{id} - Récupérer un produit par ID
    @GetMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> getProduct(@PathVariable Long id, Authentication authentication) {
        String userName = authentication.getName();
        return productRepository.findById(id)
                .filter(product -> product.getUserName().equals(userName))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /pantry/api/products - Ajouter un nouveau produit
    @PostMapping("/api/products")
    @ResponseBody
    public ResponseEntity<Product> createProduct(@RequestBody Map<String, Object> payload, Authentication authentication) {
        try {
            String userName = authentication.getName();

            String name = (String) payload.get("name");
            int quantity = payload.get("quantity") instanceof Integer
                    ? (Integer) payload.get("quantity")
                    : Integer.parseInt(payload.get("quantity").toString());
            String unit = (String) payload.get("unit");
            LocalDate expiryDate = LocalDate.parse((String) payload.get("expiryDate"));
            String storageType = (String) payload.getOrDefault("storageType", "placard");

            Product product = new Product(name, quantity, unit, expiryDate, storageType, userName);
            Product saved = productRepository.save(product);

            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // PUT /pantry/api/products/{id} - Modifier un produit
    @PutMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload, Authentication authentication) {
        String userName = authentication.getName();

        return productRepository.findById(id)
                .filter(product -> product.getUserName().equals(userName))
                .map(product -> {
                    if (payload.containsKey("name")) {
                        product.setName((String) payload.get("name"));
                    }
                    if (payload.containsKey("quantity")) {
                        product.setQuantity(payload.get("quantity") instanceof Integer
                                ? (Integer) payload.get("quantity")
                                : Integer.parseInt(payload.get("quantity").toString()));
                    }
                    if (payload.containsKey("unit")) {
                        product.setUnit((String) payload.get("unit"));
                    }
                    if (payload.containsKey("expiryDate")) {
                        product.setExpiryDate(LocalDate.parse((String) payload.get("expiryDate")));
                    }
                    if (payload.containsKey("storageType")) {
                        product.setStorageType((String) payload.get("storageType"));
                    }
                    return ResponseEntity.ok(productRepository.save(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /pantry/api/products/{id} - Supprimer un produit
    @DeleteMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteProductApi(@PathVariable Long id, Authentication authentication) {
        String userName = authentication.getName();

        return productRepository.findById(id)
                .filter(product -> product.getUserName().equals(userName))
                .map(product -> {
                    productRepository.deleteById(id);
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Produit supprimé avec succès");
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}