<template>
  <section class="section">
    <div class="container">
      <!-- Header -->
      <div class="level mb-5">
        <div class="level-left">
          <div class="level-item">
            <div>
              <h1 class="title is-2">
                <span class="icon"><i class="fas fa-utensils"></i></span>
                Mes Recettes
              </h1>
              <p class="subtitle">Recettes selectionnees pour votre plan</p>
            </div>
          </div>
        </div>
        <div class="level-right">
          <div class="level-item">
            <span class="tag is-primary is-large">
              <i class="fas fa-bookmark mr-2"></i>
              {{ recipes.length }} recette(s)
            </span>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="has-text-centered" style="padding: 4rem 0;">
        <span class="icon is-large">
          <i class="fas fa-spinner fa-spin fa-3x" style="color: var(--eco-green);"></i>
        </span>
        <p class="mt-4" style="color: var(--light-text);">Chargement des recettes...</p>
      </div>

      <!-- Message si vide -->
      <div v-else-if="recipes.length === 0" class="has-text-centered" style="padding: 4rem 0;">
        <span class="icon is-large mb-4">
          <i class="fas fa-book-open fa-3x" style="color: var(--light-text);"></i>
        </span>
        <p class="title is-5" style="color: var(--light-text);">Aucune recette selectionnee</p>
        <p style="color: var(--light-text);">Selectionnez des recettes depuis le site web pour les voir ici</p>
      </div>

      <!-- Liste des recettes -->
      <div v-else class="recipes-grid">
        <div v-for="recipe in recipes" :key="recipe.recipeId" class="recipe-card box">
          <div class="recipe-header">
            <h3 class="title is-5 mb-2">{{ recipe.name }}</h3>
            <span class="tag" :class="getCategoryClass(recipe.category)">
              {{ recipe.category || 'Non classe' }}
            </span>
          </div>

          <p class="recipe-description" v-if="recipe.description">
            {{ truncate(recipe.description, 100) }}
          </p>

          <div class="recipe-meta">
            <span v-if="recipe.preparationTime">
              <i class="fas fa-clock"></i> {{ recipe.preparationTime }} min prep
            </span>
            <span v-if="recipe.cookingTime">
              <i class="fas fa-fire"></i> {{ recipe.cookingTime }} min cuisson
            </span>
            <span v-if="recipe.servings">
              <i class="fas fa-users"></i> {{ recipe.servings }} pers.
            </span>
          </div>

          <!-- Barre de progression ingredients -->
          <div class="ingredients-progress mt-3">
            <div class="progress-label">
              <span>Ingredients disponibles</span>
              <span class="has-text-weight-semibold">{{ recipe.matchPercentage || 0 }}%</span>
            </div>
            <progress class="progress is-small" :class="getProgressClass(recipe.matchPercentage)"
                      :value="recipe.matchPercentage || 0" max="100">
            </progress>
          </div>

          <!-- Ingredients manquants -->
          <div v-if="recipe.missingIngredients && recipe.missingIngredients.length > 0" class="missing-ingredients mt-3">
            <p class="has-text-weight-semibold is-size-7 mb-2" style="color: var(--eco-orange);">
              <i class="fas fa-exclamation-triangle"></i> Ingredients manquants :
            </p>
            <div class="tags">
              <span v-for="ing in recipe.missingIngredients.slice(0, 5)" :key="ing.name" class="tag is-warning is-light">
                {{ ing.name }}
              </span>
              <span v-if="recipe.missingIngredients.length > 5" class="tag is-light">
                +{{ recipe.missingIngredients.length - 5 }} autres
              </span>
            </div>
          </div>

          <!-- Actions -->
          <div class="recipe-actions mt-4">
            <router-link :to="`/recipes/${recipe.recipeId}`" class="button is-primary is-small">
              <span class="icon"><i class="fas fa-eye"></i></span>
              <span>Voir details</span>
            </router-link>
            <button class="button is-danger is-light is-small" @click="removeRecipe(recipe.recipeId)">
              <span class="icon"><i class="fas fa-times"></i></span>
              <span>Retirer</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import { saveAllRecipes, getAllRecipes, deleteRecipe as deleteRecipeFromDB } from '@/services/db'

export default {
  name: 'RecipesView',
  data() {
    return {
      recipes: [],
      loading: true,
      isOnline: navigator.onLine
    }
  },
  mounted() {
    window.addEventListener('online', this.handleOnline)
    window.addEventListener('offline', this.handleOffline)
    this.fetchRecipes()
  },
  beforeUnmount() {
    window.removeEventListener('online', this.handleOnline)
    window.removeEventListener('offline', this.handleOffline)
  },
  methods: {
    handleOnline() {
      this.isOnline = true
      this.fetchRecipes()
    },
    handleOffline() {
      this.isOnline = false
    },
    async fetchRecipes() {
      this.loading = true
      try {
        if (this.isOnline) {
          const response = await fetch('/api/vue/recipes/selected', { credentials: 'include' })
          if (response.ok) {
            this.recipes = await response.json()
            // Sauvegarder en cache (en arriere-plan, ne pas bloquer)
            saveAllRecipes(this.recipes).catch(err => {
              console.warn('Erreur sauvegarde cache recettes:', err)
            })
          } else {
            // API a echoue, charger depuis le cache
            this.recipes = await getAllRecipes()
          }
        } else {
          // Mode hors ligne
          this.recipes = await getAllRecipes()
        }
      } catch (e) {
        console.error('Erreur chargement recettes:', e)
        // En cas d'erreur reseau, charger depuis le cache
        try {
          this.recipes = await getAllRecipes()
        } catch (cacheErr) {
          console.error('Erreur cache recettes:', cacheErr)
          this.recipes = []
        }
      } finally {
        this.loading = false
      }
    },
    async removeRecipe(recipeId) {
      if (!confirm('Retirer cette recette de votre selection ?')) return

      try {
        if (this.isOnline) {
          const response = await fetch(`/api/vue/recipes/selected/${recipeId}`, {
            method: 'DELETE',
            credentials: 'include'
          })
          if (response.ok) {
            this.recipes = this.recipes.filter(r => r.recipeId !== recipeId)
            await deleteRecipeFromDB(recipeId)
          }
        } else {
          await deleteRecipeFromDB(recipeId)
          this.recipes = this.recipes.filter(r => r.recipeId !== recipeId)
        }
      } catch (e) {
        console.error('Erreur suppression recette:', e)
      }
    },
    truncate(text, length) {
      if (!text) return ''
      return text.length > length ? text.substring(0, length) + '...' : text
    },
    getCategoryClass(category) {
      const categories = {
        'Entree': 'is-info',
        'Plat': 'is-success',
        'Dessert': 'is-warning',
        'Boisson': 'is-link',
        'Snack': 'is-primary'
      }
      return categories[category] || 'is-light'
    },
    getProgressClass(percentage) {
      if (percentage >= 80) return 'is-success'
      if (percentage >= 50) return 'is-warning'
      return 'is-danger'
    }
  }
}
</script>

<style scoped>
.recipes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.recipe-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.recipe-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.recipe-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.recipe-description {
  color: var(--light-text);
  font-size: 0.9rem;
  margin: 0.75rem 0;
}

.recipe-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.85rem;
  color: var(--light-text);
}

.recipe-meta span {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  margin-bottom: 0.25rem;
}

.recipe-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
