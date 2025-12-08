<div align="center">
  <img src="src/main/resources/static/images/EcoCookaligner.svg" alt="ECO Cook Logo" width="400">
</div>

# ECO Cook

Application web de gestion de garde-manger anti-gaspillage.

## Vue d'ensemble

ECO Cook est une Progressive Web Application (PWA) qui permet aux utilisateurs de gérer leur garde-manger, suivre les dates de péremption et découvrir des recettes adaptées en fonction de ce qui reste dans les placards. L'application fonctionne en mode connecté et hors ligne.

**Accès :**
- Application : http://localhost:8080
- Console H2 : http://localhost:8080/h2-console
  - URL JDBC : `jdbc:h2:file:./data/ecocook`
  - Username : `sa`
  - Password : *(vide)*

## Fonctionnalités Online/Offline

### Fonctionnalités Online

#### Synchronisation et APIs
- **Scan de codes-barres** : Récupération des informations produits via l'API OpenFoodFacts
- **Base de données cloud** : Synchronisation automatique des données utilisateur
- **Mises à jour en temps réel** : Nouvelles recettes et produits disponibles instantanément
- **Partage familial** : Synchronisation du garde-manger entre plusieurs comptes
- **Export de données** : Génération de listes de courses PDF/HTML
- **Notifications push** : Alertes de péremption envoyées sur tous les appareils

#### Fonctionnalités communautaires
- Consultation et partage de recettes utilisateurs
- Statistiques globales de réduction du gaspillage
- Mise à jour automatique de la base de produits

### Fonctionnalités Offline

#### Mode hors connexion complet
- **Consultation du garde-manger** : Accès complet à tous vos produits stockés localement
- **Ajout de produits** : Saisie manuelle avec synchronisation ultérieure
- **Gestion des dates** : Modification des dates de péremption et quantités
- **Recettes favorites** : Consultation des recettes précédemment sauvegardées
- **Historique de scan** : File d'attente des codes-barres à synchroniser
- **Liste de courses** : Création et modification en mode déconnecté

#### Stockage local
- Cache des produits fréquemment utilisés
- Les dernières recettes consultées
- Historique complet du garde-manger
- Files d'attente de synchronisation

## Technologies

- Java 21
- Spring Boot 3.2
- Spring Security
- Spring Data JPA
- Thymeleaf 
- Bulma CSS
- H2
- IndexedDB (stockage hors ligne)

## Pages

### Public
- `/` - Accueil
- `/login` - Connexion
- `/register` - Inscription

### Authentifié
- `/pantry` - Garde-manger
- `/scanner` - Scanner de code-barres
- `/shopping-list` - Liste de courses
- `/recipes/new` - Créer une recette
- `/profile` - Profil utilisateur

## API REST

### Catalogue de produits
- `GET /api/products/search?query={recherche}` - Autocomplete produits
- `GET /api/products/details?name={nom}` - Détails d'un produit
- `GET /api/products/all` - Liste complète du catalogue

### Scanner
- `GET /scanner/api/scan/{barcode}` - Scanner un code-barres (OpenFoodFacts)

## Entités

- **User** : userName, displayName, password, roles
- **Product** : Produits du garde-manger avec unités et stockage
- **ProductCatalog** : Catalogue de produits standards (45+ items)
- **Recipe** : Recettes avec ingrédients
- **Ingredient** : Ingrédients des recettes
- **RecipeSelection** : Recettes sélectionnées par utilisateur
- **ShoppingListItem** : Articles de la liste de courses

## Lancement
```bash
mvn spring-boot:run
```

Ou :
```bash
mvn clean package -DskipTests
java -jar target/ecocook-1.0.0.jar
```
