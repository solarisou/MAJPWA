package com.ecocook.controller;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecocook.model.ProductCatalog;
import com.ecocook.repository.ProductCatalogRepository;

@RestController
@RequestMapping("/api/products")
public class ProductCatalogController {

    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @GetMapping("/search")
    public List<ProductSuggestion> searchProducts(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        String normalizedQuery = normalize(query);
        Map<String, ProductSuggestion> suggestions = new LinkedHashMap<>();

        productCatalogRepository
            .findByDisplayNameFrContainingIgnoreCaseOrderByDisplayNameFr(query)
            .forEach(product -> addSuggestion(suggestions, resolveDisplayName(product), product));

        if (suggestions.size() < 10) {
            productCatalogRepository
                .findByNameContainingIgnoreCaseOrderByName(query)
                .forEach(product -> addSuggestion(suggestions, resolveDisplayName(product), product));
        }

        if (suggestions.size() < 10) {
            productCatalogRepository.findAll().stream()
                .filter(product -> {
                    String name = resolveDisplayName(product);
                    if (name == null) {
                        return false;
                    }
                    String normalizedName = normalize(name);
                    return normalizedName.contains(normalizedQuery);
                })
                .sorted(Comparator.comparing(this::resolveDisplayName, String.CASE_INSENSITIVE_ORDER))
                .forEach(product -> {
                    if (suggestions.size() < 10) {
                        addSuggestion(suggestions, resolveDisplayName(product), product);
                    }
                });
        }

        // Ajouter des variantes sans ligatures pour aider la saisie
        Map<String, ProductSuggestion> expandedSuggestions = new LinkedHashMap<>(suggestions);
        suggestions.forEach((displayName, suggestion) -> {
            String variant = replaceLigatures(displayName);
            if (!variant.equals(displayName)) {
                expandedSuggestions.putIfAbsent(variant, new ProductSuggestion(
                        variant,
                        suggestion.getTechnicalName(),
                        suggestion.getIconPath(),
                        suggestion.getDefaultUnit(),
                        suggestion.getDefaultStorageType()
                ));
            }
        });

        return expandedSuggestions.values().stream()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/details")
    public ProductCatalog getProductDetails(@RequestParam String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        return productCatalogRepository.findByDisplayNameFrContainingIgnoreCaseOrderByDisplayNameFr(name).stream()
                .filter(product -> normalize(resolveDisplayName(product)).equals(normalize(name))
                        || normalize(product.getName()).equals(normalize(name)))
                .findFirst()
                .orElseGet(() -> productCatalogRepository.findAll().stream()
                        .filter(product -> normalize(resolveDisplayName(product)).equals(normalize(name))
                                || normalize(product.getName()).equals(normalize(name)))
                        .findFirst()
                        .orElse(null));
    }
    
    @GetMapping("/all")
    public List<ProductCatalog> getAllProducts() {
        return productCatalogRepository.findAll();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        lower = lower.replace("œ", "oe").replace("æ", "ae");
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private String replaceLigatures(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("Œ", "Oe")
            .replace("œ", "oe")
            .replace("Æ", "Ae")
            .replace("æ", "ae");
    }
    
    private String resolveDisplayName(ProductCatalog product) {
        if (product == null) {
            return "";
        }
        if (product.getDisplayNameFr() != null && !product.getDisplayNameFr().isBlank()) {
            return product.getDisplayNameFr();
        }
        return product.getName() != null ? product.getName() : "";
    }

    private void addSuggestion(Map<String, ProductSuggestion> suggestions, String displayName, ProductCatalog product) {
        if (displayName == null || displayName.isBlank()) {
            return;
        }
        suggestions.putIfAbsent(displayName, mapToSuggestion(displayName, product));
    }

    private ProductSuggestion mapToSuggestion(String displayName, ProductCatalog product) {
        return new ProductSuggestion(
                displayName,
                product.getName(),
                product.getIconPath(),
                product.getDefaultUnit(),
                product.getDefaultStorageType()
        );
    }

    public static class ProductSuggestion {
        private final String displayName;
        private final String technicalName;
        private final String iconPath;
        private final String defaultUnit;
        private final String defaultStorageType;

        public ProductSuggestion(String displayName, String technicalName, String iconPath, String defaultUnit, String defaultStorageType) {
            this.displayName = displayName;
            this.technicalName = technicalName;
            this.iconPath = iconPath;
            this.defaultUnit = defaultUnit;
            this.defaultStorageType = defaultStorageType;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTechnicalName() {
            return technicalName;
        }

        public String getIconPath() {
            return iconPath;
        }

        public String getDefaultUnit() {
            return defaultUnit;
        }

        public String getDefaultStorageType() {
            return defaultStorageType;
        }
    }
}

