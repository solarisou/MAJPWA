package com.ecocook.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ecocook.model.Product;
import com.ecocook.repository.ProductRepository;
import com.ecocook.service.OpenFoodFactsService;

@Controller
@RequestMapping("/scanner")
public class ScannerController {

    @Autowired
    private OpenFoodFactsService openFoodFactsService;
    
    @Autowired
    private ProductRepository productRepository;
    
   // Page du scanner
    @GetMapping
    public String scannerPage(Model model) {
        return "scanner/scanner";
    }
    // API pour scanner un code-barres
    @GetMapping("/api/scan/{barcode}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> scanBarcode(@PathVariable String barcode) {
        Map<String, Object> productInfo = openFoodFactsService.getProductInfo(barcode);
        
        if (productInfo != null) {
            return ResponseEntity.ok(productInfo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Ajouter un produit scanné au garde-manger
    @PostMapping("/add")
    public String addScannedProduct(
            @RequestParam String name,
            @RequestParam int quantity,
            @RequestParam String expiryDate,
            @RequestParam String storageType,
            @RequestParam(required = false) String barcode,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            String userName = authentication.getName();
            Product product = new Product(name, quantity, LocalDate.parse(expiryDate), storageType, userName);
            productRepository.save(product);
            
            redirectAttributes.addFlashAttribute("success", "Produit scanné ajouté avec succès !");
            return "redirect:/pantry";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'ajout : " + e.getMessage());
            return "redirect:/scanner";
        }
    }
}

