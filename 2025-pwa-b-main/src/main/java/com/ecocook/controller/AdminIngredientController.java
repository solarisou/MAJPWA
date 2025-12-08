package com.ecocook.controller;

import com.ecocook.model.ProductCatalog;
import com.ecocook.repository.ProductCatalogRepository;
import com.ecocook.service.IngredientIconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;

@Controller
@RequestMapping("/admin/ingredients")
public class AdminIngredientController {

    @Autowired
    private ProductCatalogRepository productCatalogRepository;

    @Autowired
    private IngredientIconService ingredientIconService;

    @GetMapping
    public String listIngredients(Model model) {
        model.addAttribute("ingredients", productCatalogRepository.findAll(Sort.by(Sort.Direction.ASC, "displayNameFr")));
        try {
            model.addAttribute("availableIcons", ingredientIconService.listIcons());
        } catch (IOException e) {
            model.addAttribute("availableIconsError", "Impossible de charger la liste des icônes : " + e.getMessage());
        }
        model.addAttribute("newIngredient", new ProductCatalog());
        return "admin/ingredients";
    }

    @PostMapping
    public String createIngredient(@RequestParam String displayNameFr,
                                   @RequestParam(required = false) String technicalName,
                                   @RequestParam String category,
                                   @RequestParam(required = false, defaultValue = "placard") String defaultStorageType,
                                   @RequestParam(required = false, defaultValue = "0") Integer defaultShelfLife,
                                   @RequestParam(required = false) String defaultUnit,
                                   @RequestParam(required = false) MultipartFile iconFile,
                                   @RequestParam(required = false) String iconChoice,
                                   RedirectAttributes redirectAttributes) {
        try {
            ProductCatalog ingredient = new ProductCatalog();
            populateIngredient(ingredient, displayNameFr, technicalName, category, defaultStorageType, defaultShelfLife, defaultUnit);
            applyIcon(ingredient, iconFile, iconChoice);
            productCatalogRepository.save(ingredient);
            redirectAttributes.addFlashAttribute("success", "Ingrédient ajouté avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création de l'ingrédient : " + e.getMessage());
        }
        return "redirect:/admin/ingredients";
    }

    @PostMapping("/{id}/update")
    public String updateIngredient(@PathVariable Long id,
                                   @RequestParam String displayNameFr,
                                   @RequestParam(required = false) String technicalName,
                                   @RequestParam String category,
                                   @RequestParam(required = false, defaultValue = "placard") String defaultStorageType,
                                   @RequestParam(required = false, defaultValue = "0") Integer defaultShelfLife,
                                   @RequestParam(required = false) String defaultUnit,
                                   @RequestParam(required = false) MultipartFile iconFile,
                                   @RequestParam(required = false) String iconChoice,
                                   RedirectAttributes redirectAttributes) {
        return productCatalogRepository.findById(id)
            .map(ingredient -> {
                try {
                    populateIngredient(ingredient, displayNameFr, technicalName, category, defaultStorageType, defaultShelfLife, defaultUnit);
                    applyIcon(ingredient, iconFile, iconChoice);
                    productCatalogRepository.save(ingredient);
                    redirectAttributes.addFlashAttribute("success", "Ingrédient mis à jour.");
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("error", "Erreur lors de la mise à jour : " + e.getMessage());
                }
                return "redirect:/admin/ingredients";
            })
            .orElseGet(() -> {
                redirectAttributes.addFlashAttribute("error", "Ingrédient introuvable.");
                return "redirect:/admin/ingredients";
            });
    }

    @PostMapping("/{id}/delete")
    public String deleteIngredient(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        if (productCatalogRepository.existsById(id)) {
            productCatalogRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Ingrédient supprimé.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ingrédient introuvable.");
        }
        return "redirect:/admin/ingredients";
    }

    private void populateIngredient(ProductCatalog ingredient,
                                    String displayNameFr,
                                    String technicalName,
                                    String category,
                                    String defaultStorageType,
                                    Integer defaultShelfLife,
                                    String defaultUnit) {
        if (!StringUtils.hasText(displayNameFr)) {
            throw new IllegalArgumentException("Le nom en français est obligatoire.");
        }
        ingredient.setDisplayNameFr(displayNameFr.trim());
        ingredient.setName(StringUtils.hasText(technicalName)
                ? sanitizeTechnicalName(technicalName)
                : generateTechnicalName(displayNameFr));
        ingredient.setCategory(category);
        ingredient.setDefaultStorageType(StringUtils.hasText(defaultStorageType) ? defaultStorageType : "placard");
        ingredient.setDefaultShelfLife(defaultShelfLife != null && defaultShelfLife >= 0 ? defaultShelfLife : 0);
        ingredient.setDefaultUnit(StringUtils.hasText(defaultUnit) ? defaultUnit.trim() : null);
    }

    private void applyIcon(ProductCatalog ingredient,
                           MultipartFile iconFile,
                           String iconChoice) throws IOException {
        if (iconFile != null && !iconFile.isEmpty()) {
            String savedPath = ingredientIconService.saveIcon(iconFile);
            ingredient.setIconPath(savedPath);
        } else if (StringUtils.hasText(iconChoice)) {
            ingredient.setIconPath(iconChoice.trim());
        }
    }

    private String sanitizeTechnicalName(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^a-z0-9\\-\\s]", "");
        normalized = normalized.replaceAll("\\s+", "-");
        normalized = normalized.replaceAll("-{2,}", "-");
        normalized = normalized.replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "ingredient" : normalized;
    }

    private String generateTechnicalName(String displayName) {
        return sanitizeTechnicalName(displayName);
    }
}



