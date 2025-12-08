package com.ecocook.init;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.ecocook.model.Ingredient;
import com.ecocook.model.ProductCatalog;
import com.ecocook.model.Recipe;
import com.ecocook.model.user.User;
import com.ecocook.model.user.UserRole;
import com.ecocook.model.user.UserRepository;
import com.ecocook.repository.ProductCatalogRepository;
import com.ecocook.repository.RecipeRepository;
import com.ecocook.secu.UserService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RecipeRepository recipeRepository;
    
    @Autowired
    private ProductCatalogRepository productCatalogRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;
    
    @Override
    public void run(String... args) {
        // Créer un compte admin par défaut s'il n'existe pas
        createAdminAccount();
        createNamedAdmins();
        
        if (productCatalogRepository.count() == 0) {
            initializeProductCatalog();
        }
        
        if (recipeRepository.count() == 0) {
            createSampleRecipes();
        }
    }
    
    private void createAdminAccount() {
        String adminUsername = "admin";
        if (!userRepository.existsById(adminUsername)) {
            User admin = new User();
            admin.setUserName(adminUsername);
            admin.setDisplayName("Administrateur");
            admin.getRoles().add(UserRole.ADMIN);
            admin.getRoles().add(UserRole.USER); // Admin a aussi le rôle USER
            
            // Mot de passe par défaut : "admin123"
            userService.saveUserComputingDerivedPassword(admin, "admin123");
            
            System.out.println("✓ Compte administrateur créé :");
            System.out.println("  - Username: admin");
            System.out.println("  - Password: admin123");
            System.out.println("  ⚠️  IMPORTANT: Changez le mot de passe après la première connexion !");
        } else {
            System.out.println("✓ Compte administrateur existe déjà");
        }
    }

    private void createNamedAdmins() {
        createAdminIfMissing("salman", "Salman", "salman123");
        createAdminIfMissing("mtara", "Mtara", "mtara123");
        createAdminIfMissing("andrea", "Andrea", "andrea123");
    }

    private void createAdminIfMissing(String username, String displayName, String rawPassword) {
        Objects.requireNonNull(username, "username must not be null");

        if (userRepository.existsById(username)) {
            return;
        }

        User admin = new User();
        admin.setUserName(username);
        admin.setDisplayName(displayName != null ? displayName : username);
        admin.getRoles().add(UserRole.ADMIN);
        admin.getRoles().add(UserRole.USER);

        userService.saveUserComputingDerivedPassword(admin, rawPassword);

        System.out.println("✓ Compte administrateur créé :");
        System.out.println("  - Username: " + username);
        System.out.println("  - Password: " + rawPassword);
        System.out.println("  ⚠️  IMPORTANT: Changez le mot de passe après la première connexion !");
    }
    
    private void initializeProductCatalog() {
        saveProduct("Tomates", "Légumes", "panier", 7, "kg", "tomato");
        saveProduct("Pommes", "Fruits", "panier", 14, "kg", "apple");
        saveProduct("Bananes", "Fruits", "panier", 7, "kg", "banana");
        saveProduct("Carottes", "Légumes", "frigo", 14, "kg", "carrot");
        saveProduct("Oignons", "Légumes", "placard", 30, "kg", "onion");
        saveProduct("Pommes de terre", "Légumes", "placard", 21, "kg", "potato");
        
        saveProduct("Lait", "Produits laitiers", "frigo", 7, "L", "milk");
        saveProduct("Yaourt", "Produits laitiers", "frigo", 21, "unité", "yogurt");
        saveProduct("Fromage", "Produits laitiers", "frigo", 21, "g", "cheese");
        saveProduct("Beurre", "Produits laitiers", "frigo", 30, "g", "butter");
        saveProduct("Crème fraîche", "Produits laitiers", "frigo", 14, "L", "cream");
        
        saveProduct("Pain", "Boulangerie", "placard", 3, "unité", "bread");
        saveProduct("Baguette", "Boulangerie", "placard", 2, "unité", "baguette");
        saveProduct("Brioche", "Boulangerie", "placard", 7, "unité", "brioche");
        
        saveProduct("Pâtes", "Épicerie", "placard", 365, "g", "pasta");
        saveProduct("Riz", "Épicerie", "placard", 365, "g", "rice");
        saveProduct("Farine", "Épicerie", "placard", 365, "g", "flour");
        saveProduct("Sucre", "Épicerie", "placard", 365, "g", "sugar");
        saveProduct("Sel", "Épicerie", "placard", 730, "g", "salt");
        saveProduct("Huile", "Épicerie", "placard", 365, "L", "oil");
        saveProduct("Huile d'olive", "Épicerie", "placard", 365, "L", "olive-oil");
        
        saveProduct("Poulet", "Viandes", "frigo", 3, "g", "chicken");
        saveProduct("Bœuf", "Viandes", "frigo", 3, "g", "beef");
        saveProduct("Porc", "Viandes", "frigo", 3, "g", "pork");
        saveProduct("Jambon", "Viandes", "frigo", 7, "tranches", "ham");
        
        saveProduct("Saumon", "Poissons", "frigo", 2, "g", "salmon");
        saveProduct("Thon", "Poissons", "frigo", 2, "g", "tuna");
        saveProduct("Cabillaud", "Poissons", "frigo", 2, "g", "cod");
        
        saveProduct("Œufs", "Produits laitiers", "frigo", 28, "unité", "egg");
        
        saveProduct("Eau", "Boissons", "placard", 365, "L", "water");
        saveProduct("Jus d'orange", "Boissons", "frigo", 7, "L", "orange-juice");
        saveProduct("Lait de coco", "Boissons", "placard", 365, "mL", "coconut-milk");
        
        saveProduct("Chocolat", "Sucreries", "placard", 180, "g", "chocolate");
        saveProduct("Confiture", "Sucreries", "placard", 180, "g", "jam");
        saveProduct("Miel", "Sucreries", "placard", 730, "g", "honey");
        
        saveProduct("Ail", "Condiments", "placard", 60, "gousse", "garlic");
        saveProduct("Basilic", "Condiments", "frigo", 7, "bouquet", "basil");
        saveProduct("Persil", "Condiments", "frigo", 7, "bouquet", "parsley");
        saveProduct("Thym", "Condiments", "placard", 365, "g", "thyme");
        
        saveProduct("Glace", "Surgelés", "congelateur", 180, "L", "ice-cream");
        saveProduct("Légumes surgelés", "Surgelés", "congelateur", 365, "g", "frozen-vegetables");
        saveProduct("Pizza surgelée", "Surgelés", "congelateur", 180, "unité", "frozen-pizza");
        
        System.out.println("✓ Catalogue de produits initialisé avec " + productCatalogRepository.count() + " produits");
    }
    
    private void saveProduct(String name, String category, String storage, Integer shelfLife, String unit, String icon) {
        ProductCatalog product = new ProductCatalog(name, category, storage, shelfLife, unit);
        product.setDisplayNameFr(name);
        String iconRelativePath = "/uploads/ingredient-icons/" + icon + ".svg";
        java.nio.file.Path absolutePath = java.nio.file.Paths.get("src/main/resources/static" + iconRelativePath);
        if (java.nio.file.Files.exists(absolutePath)) {
            product.setIconPath(iconRelativePath);
        }
        productCatalogRepository.save(product);
    }
    
    private void createSampleRecipes() {
        // Recette 1 : Omelette aux tomates
        Recipe omelette = new Recipe(
            "Omelette aux tomates",
            "Une omelette simple et délicieuse aux tomates fraîches",
            "1. Battre les oeufs dans un bol\n2. Couper les tomates en dés\n3. Chauffer l'huile dans une poêle\n4. Verser les oeufs et ajouter les tomates\n5. Cuire à feu moyen pendant 3-4 minutes",
            5, 5, 2,
            "facile", "plat"
        );
        omelette.addIngredient(new Ingredient("oeufs", 3, "pièce"));
        omelette.addIngredient(new Ingredient("Tomates", 2, "pièce"));
        omelette.addIngredient(new Ingredient("Huile", 1, "cuillère"));
        omelette.addIngredient(new Ingredient("Sel", 1, "pincée"));
        recipeRepository.save(omelette);
        
        // Recette 2 : Salade de tomates mozza
        Recipe salade = new Recipe(
            "Salade tomates mozzarella",
            "Salade fraîche italienne classique",
            "1. Couper les tomates en rondelles\n2. Couper la mozzarella en tranches\n3. Alterner tomates et mozzarella dans l'assiette\n4. Ajouter basilic, huile d'olive, sel et poivre",
            10, 0, 2,
            "facile", "entrée"
        );
        salade.addIngredient(new Ingredient("Tomates", 3, "pièce"));
        salade.addIngredient(new Ingredient("Mozzarella", 1, "boule"));
        salade.addIngredient(new Ingredient("Basilic", 5, "feuilles"));
        salade.addIngredient(new Ingredient("Huile d'olive", 2, "cuillères"));
        recipeRepository.save(salade);
        
        // Recette 3 : Pâtes à la tomate
        Recipe pates = new Recipe(
            "Pâtes sauce tomate maison",
            "Des pâtes simples avec une sauce tomate fraîche",
            "1. Faire bouillir de l'eau salée\n2. Cuire les pâtes selon les instructions\n3. Faire revenir l'ail dans l'huile\n4. Ajouter les tomates coupées\n5. Laisser mijoter 10 minutes\n6. Mélanger avec les pâtes",
            10, 15, 4,
            "facile", "plat"
        );
        pates.addIngredient(new Ingredient("Pâtes", 400, "g"));
        pates.addIngredient(new Ingredient("Tomates", 6, "pièce"));
        pates.addIngredient(new Ingredient("Ail", 2, "gousses"));
        pates.addIngredient(new Ingredient("Huile d'olive", 3, "cuillères"));
        pates.addIngredient(new Ingredient("Basilic", 10, "feuilles"));
        recipeRepository.save(pates);
        
        // Recette 4 : Tarte aux pommes
        Recipe tarte = new Recipe(
            "Tarte aux pommes",
            "Tarte aux pommes classique française",
            "1. Préchauffer le four à 180°C\n2. Étaler la pâte dans un moule\n3. Éplucher et couper les pommes en lamelles\n4. Disposer les pommes sur la pâte\n5. Saupoudrer de sucre\n6. Cuire 35 minutes",
            15, 35, 6,
            "moyen", "dessert"
        );
        tarte.addIngredient(new Ingredient("Pâte brisée", 1, "rouleau"));
        tarte.addIngredient(new Ingredient("Pommes", 4, "pièce"));
        tarte.addIngredient(new Ingredient("Sucre", 3, "cuillères"));
        tarte.addIngredient(new Ingredient("Beurre", 20, "g"));
        recipeRepository.save(tarte);
        
        System.out.println(" 4 recettes d'exemple créées !");
    }
}