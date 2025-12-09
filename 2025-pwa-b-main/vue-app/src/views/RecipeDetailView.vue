<template>
  <section class="section recipe-detail-section">
    <div class="container">
      <!-- Bouton retour -->
      <router-link to="/recipes" class="button is-light mb-4">
        <span class="icon"><i class="fas fa-arrow-left"></i></span>
        <span>Retour aux recettes</span>
      </router-link>

      <!-- Loading -->
      <div v-if="loading" class="has-text-centered" style="padding: 4rem 0;">
        <span class="icon is-large">
          <i class="fas fa-spinner fa-spin fa-3x" style="color: var(--eco-green);"></i>
        </span>
        <p class="mt-4" style="color: var(--light-text);">Chargement de la recette...</p>
      </div>

      <!-- Recette non trouvee -->
      <div v-else-if="!recipe" class="has-text-centered" style="padding: 4rem 0;">
        <span class="icon is-large mb-4">
          <i class="fas fa-question-circle fa-3x" style="color: var(--light-text);"></i>
        </span>
        <p class="title is-5" style="color: var(--light-text);">Recette non trouvee</p>
      </div>

      <!-- Contenu de la recette -->
      <div v-else class="recipe-content">
        <!-- Header -->
        <div class="recipe-header box">
          <div class="header-top">
            <h1 class="title is-2">{{ recipe.name }}</h1>
            <div class="tags">
              <span class="tag is-medium" :class="getCategoryClass(recipe.category)">
                {{ recipe.category || 'Non classe' }}
              </span>
              <span class="tag is-medium" :class="getDifficultyClass(recipe.difficulty)">
                {{ recipe.difficulty || 'Non defini' }}
              </span>
            </div>
          </div>

          <p class="recipe-description" v-if="recipe.description">
            {{ recipe.description }}
          </p>

          <!-- Meta infos -->
          <div class="recipe-meta">
            <div class="meta-item" v-if="recipe.preparationTime">
              <span class="icon"><i class="fas fa-clock"></i></span>
              <div>
                <span class="meta-label">Preparation</span>
                <span class="meta-value">{{ recipe.preparationTime }} min</span>
              </div>
            </div>
            <div class="meta-item" v-if="recipe.cookingTime">
              <span class="icon"><i class="fas fa-fire"></i></span>
              <div>
                <span class="meta-label">Cuisson</span>
                <span class="meta-value">{{ recipe.cookingTime }} min</span>
              </div>
            </div>
            <div class="meta-item" v-if="recipe.servings">
              <span class="icon"><i class="fas fa-users"></i></span>
              <div>
                <span class="meta-label">Portions</span>
                <span class="meta-value">{{ recipe.servings }} pers.</span>
              </div>
            </div>
            <div class="meta-item" v-if="recipe.preparationTime || recipe.cookingTime">
              <span class="icon"><i class="fas fa-hourglass-half"></i></span>
              <div>
                <span class="meta-label">Temps total</span>
                <span class="meta-value">{{ (recipe.preparationTime || 0) + (recipe.cookingTime || 0) }} min</span>
              </div>
            </div>
          </div>

          <!-- Barre de progression -->
          <div class="ingredients-progress mt-4" v-if="recipe.matchPercentage !== undefined">
            <div class="progress-label">
              <span>Ingredients disponibles dans le garde-manger</span>
              <span class="has-text-weight-bold">{{ recipe.matchPercentage }}%</span>
            </div>
            <progress class="progress" :class="getProgressClass(recipe.matchPercentage)"
                      :value="recipe.matchPercentage" max="100"></progress>
          </div>
        </div>

        <div class="columns">
          <!-- Ingredients -->
          <div class="column is-4">
            <div class="box ingredients-box">
              <h2 class="title is-4">
                <span class="icon"><i class="fas fa-carrot"></i></span>
                Ingredients
              </h2>

              <ul class="ingredients-list">
                <li v-for="(ing, index) in recipe.ingredients" :key="index"
                    :class="getIngredientClass(ing.name)">
                  <span class="ingredient-status">
                    <i :class="getIngredientIcon(ing.name)"></i>
                  </span>
                  <span class="ingredient-name">{{ ing.name }}</span>
                  <span class="ingredient-qty" v-if="ing.quantity">
                    {{ ing.quantity }} {{ ing.unit }}
                  </span>
                </li>
              </ul>

              <!-- Legende -->
              <div class="legend mt-4">
                <div class="legend-item">
                  <span class="icon has-text-success"><i class="fas fa-check-circle"></i></span>
                  <span>Disponible</span>
                </div>
                <div class="legend-item">
                  <span class="icon has-text-danger"><i class="fas fa-times-circle"></i></span>
                  <span>Manquant</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Instructions -->
          <div class="column is-8">
            <div class="box instructions-box">
              <h2 class="title is-4">
                <span class="icon"><i class="fas fa-list-ol"></i></span>
                Instructions
              </h2>

              <div v-if="recipe.instructions" class="instructions-content">
                <div v-for="(step, index) in parseInstructions(recipe.instructions)" :key="index"
                     class="instruction-step">
                  <div class="step-number">{{ index + 1 }}</div>
                  <div class="step-content">{{ step }}</div>
                </div>
              </div>
              <p v-else class="has-text-grey">Aucune instruction disponible</p>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="actions-bar mt-4">
          <button class="button is-danger is-light" @click="removeRecipe">
            <span class="icon"><i class="fas fa-times"></i></span>
            <span>Retirer de ma selection</span>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import { getAllRecipes, deleteRecipe as deleteRecipeFromDB } from '@/services/db'

export default {
  name: 'RecipeDetailView',
  data() {
    return {
      recipe: null,
      loading: true,
      isOnline: navigator.onLine
    }
  },
  computed: {
    recipeId() {
      return parseInt(this.$route.params.id)
    },
    matchedIngredientNames() {
      if (!this.recipe || !this.recipe.matchedIngredients) return []
      return this.recipe.matchedIngredients.map(i => i.name.toLowerCase())
    }
  },
  mounted() {
    window.addEventListener('online', this.handleOnline)
    window.addEventListener('offline', this.handleOffline)
    this.fetchRecipe()
  },
  beforeUnmount() {
    window.removeEventListener('online', this.handleOnline)
    window.removeEventListener('offline', this.handleOffline)
  },
  methods: {
    handleOnline() {
      this.isOnline = true
    },
    handleOffline() {
      this.isOnline = false
    },
    async fetchRecipe() {
      this.loading = true
      try {
        // D'abord essayer de charger depuis le cache local
        const cachedRecipes = await getAllRecipes()
        const cached = cachedRecipes.find(r => r.recipeId === this.recipeId)

        if (this.isOnline) {
          const response = await fetch('/api/vue/recipes/selected', { credentials: 'include' })
          if (response.ok) {
            const recipes = await response.json()
            this.recipe = recipes.find(r => r.recipeId === this.recipeId)
          } else if (cached) {
            this.recipe = cached
          }
        } else {
          this.recipe = cached || null
        }
      } catch (e) {
        console.error('Erreur chargement recette:', e)
        // Fallback sur le cache
        const cachedRecipes = await getAllRecipes()
        this.recipe = cachedRecipes.find(r => r.recipeId === this.recipeId) || null
      } finally {
        this.loading = false
      }
    },
    async removeRecipe() {
      if (!confirm('Retirer cette recette de votre selection ?')) return

      try {
        if (this.isOnline) {
          const response = await fetch(`/api/vue/recipes/selected/${this.recipeId}`, {
            method: 'DELETE',
            credentials: 'include'
          })
          if (response.ok) {
            await deleteRecipeFromDB(this.recipeId)
            this.$router.push('/recipes')
          }
        } else {
          await deleteRecipeFromDB(this.recipeId)
          this.$router.push('/recipes')
        }
      } catch (e) {
        console.error('Erreur suppression recette:', e)
      }
    },
    parseInstructions(instructions) {
      if (!instructions) return []
      // Separer par lignes ou par numeros
      return instructions
        .split(/\n|(?=\d+\.\s)/)
        .map(s => s.replace(/^\d+\.\s*/, '').trim())
        .filter(s => s.length > 0)
    },
    getIngredientClass(name) {
      return this.matchedIngredientNames.includes(name.toLowerCase())
        ? 'is-available'
        : 'is-missing'
    },
    getIngredientIcon(name) {
      return this.matchedIngredientNames.includes(name.toLowerCase())
        ? 'fas fa-check-circle has-text-success'
        : 'fas fa-times-circle has-text-danger'
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
    getDifficultyClass(difficulty) {
      const difficulties = {
        'Facile': 'is-success is-light',
        'Moyen': 'is-warning is-light',
        'Difficile': 'is-danger is-light'
      }
      return difficulties[difficulty] || 'is-light'
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
.recipe-detail-section {
  background: var(--bg-light, #f9fafb);
  min-height: 100vh;
}

.recipe-header {
  margin-bottom: 1.5rem;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1rem;
}

.recipe-description {
  color: var(--light-text, #6b7280);
  font-size: 1.1rem;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.recipe-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 2rem;
  padding: 1rem 0;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.meta-item .icon {
  font-size: 1.5rem;
  color: var(--eco-orange, #d17a46);
}

.meta-label {
  display: block;
  font-size: 0.8rem;
  color: var(--light-text, #6b7280);
}

.meta-value {
  display: block;
  font-weight: 600;
  color: var(--dark-text, #374151);
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

/* Ingredients */
.ingredients-box {
  background: linear-gradient(to bottom, #fff, #fdfcfb);
}

.ingredients-box .title .icon {
  color: var(--eco-green, #6b9440);
}

.ingredients-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.ingredients-list li {
  display: flex;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px dashed var(--border-color, #e5e7eb);
  gap: 0.75rem;
}

.ingredients-list li:last-child {
  border-bottom: none;
}

.ingredient-status {
  width: 24px;
  text-align: center;
}

.ingredient-name {
  flex: 1;
  font-weight: 500;
}

.ingredient-qty {
  color: var(--eco-orange, #d17a46);
  font-weight: 600;
  font-size: 0.9rem;
}

.legend {
  display: flex;
  gap: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--light-text, #6b7280);
}

/* Instructions */
.instructions-box .title .icon {
  color: var(--eco-orange, #d17a46);
}

.instruction-step {
  display: flex;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.step-number {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, var(--eco-green, #6b9440), var(--eco-orange, #d17a46));
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
  line-height: 1.7;
  padding-top: 0.25rem;
}

.actions-bar {
  display: flex;
  justify-content: center;
  padding-top: 1rem;
}

/* Responsive */
@media (max-width: 768px) {
  .header-top {
    flex-direction: column;
  }

  .recipe-meta {
    gap: 1rem;
  }

  .meta-item {
    flex: 1 1 45%;
  }

  .columns {
    flex-direction: column;
  }

  .column.is-4,
  .column.is-8 {
    width: 100%;
  }
}
</style>
