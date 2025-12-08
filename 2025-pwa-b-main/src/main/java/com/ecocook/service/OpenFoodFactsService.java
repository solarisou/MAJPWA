package com.ecocook.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenFoodFactsService {
    
    private static final String API_URL = "https://world.openfoodfacts.org/api/v0/product/";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public OpenFoodFactsService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public Map<String, Object> getProductInfo(String barcode) {
        try {
            // Nettoyage du code-barres
            String cleanBarcode = barcode.trim().replaceAll("[^0-9]", "");
            
            String url = API_URL + cleanBarcode + ".json";
            System.out.println("Appel OpenFoodFacts: " + url);
            
            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null || response.isEmpty()) {
                System.err.println("Réponse vide de l'API OpenFoodFacts");
                return null;
            }
            
            JsonNode rootNode = objectMapper.readTree(response);
            
            if (rootNode.has("status") && rootNode.get("status").asInt() == 1) {
                JsonNode product = rootNode.get("product");
                if (product != null) {
                    System.out.println("Produit trouvé: " + product.get("product_name"));
                    return parseProductData(product);
                }
            }
            
            System.err.println("Produit non trouvé dans OpenFoodFacts (status: " + 
                             (rootNode.has("status") ? rootNode.get("status").asInt() : "inconnu") + ")");
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du produit: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private Map<String, Object> parseProductData(JsonNode product) {
        Map<String, Object> productData = new HashMap<>();
        String productName = product.has("product_name") ? product.get("product_name").asText() : "";
        String genericName = product.has("generic_name") ? product.get("generic_name").asText() : "";
        String brands = product.has("brands") ? product.get("brands").asText() : "";
        
        String normalizedName = normalizeProductName(productName, genericName, brands);
        
        productData.put("name", normalizedName);
        productData.put("originalName", productName);
        productData.put("brands", brands);
        productData.put("genericName", genericName);
        
        if (product.has("categories")) {
            productData.put("categories", product.get("categories").asText());
        }
        
        if (product.has("image_url")) {
            productData.put("imageUrl", product.get("image_url").asText());
        }
        
        if (product.has("quantity")) {
            productData.put("quantity", product.get("quantity").asText());
        }
        
        productData.put("suggestedStorage", suggestStorageType(product));
        
        return productData;
    }
    
    private String normalizeProductName(String productName, String genericName, String brands) {
        if (genericName != null && !genericName.isEmpty()) {
            return capitalizeFirst(genericName.toLowerCase());
        }
        
        if (productName != null && !productName.isEmpty()) {
            String normalized = productName;
            
            if (brands != null && !brands.isEmpty()) {
                String[] brandList = brands.split(",");
                for (String brand : brandList) {
                    normalized = normalized.replaceAll("(?i)" + brand.trim(), "").trim();
                }
            }
            
            normalized = normalized.replaceAll("(?i)(bio|naturel|naturelle|premium|original|classique)", "").trim();
            normalized = normalized.replaceAll("\\s+", " ").trim();
            
            return capitalizeFirst(normalized.toLowerCase());
        }
        
        return "Produit inconnu";
    }
    
    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    
    private String suggestStorageType(JsonNode product) {
        String categories = "";
        if (product.has("categories")) {
            categories = product.get("categories").asText().toLowerCase();
        }
        if (categories.contains("surgelés") || categories.contains("frozen") || 
            categories.contains("glace") || categories.contains("ice cream")) {
            return "congelateur";
        }
        
        if (categories.contains("frais") || categories.contains("fresh") ||
            categories.contains("lait") || categories.contains("milk") ||
            categories.contains("yaourt") || categories.contains("yogurt") ||
            categories.contains("fromage") || categories.contains("cheese") ||
            categories.contains("viande") || categories.contains("meat") ||
            categories.contains("poisson") || categories.contains("fish")) {
            return "frigo";
        }
        
        if (categories.contains("fruit") || categories.contains("légume") || 
            categories.contains("vegetable")) {
            return "panier";
        }
        
        return "placard";
    }
}

