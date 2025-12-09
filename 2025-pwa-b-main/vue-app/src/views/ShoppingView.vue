<template>
  <section class="section shopping-section">
    <div class="container">
      <!-- Loading -->
      <div v-if="loading" class="has-text-centered" style="padding: 4rem 0;">
        <span class="icon is-large">
          <i class="fas fa-spinner fa-spin fa-3x" style="color: var(--eco-green);"></i>
        </span>
        <p class="mt-4" style="color: var(--light-text);">Chargement de la liste...</p>
      </div>

      <div v-else>
        <!-- Modal pour ajouter au garde-manger -->
        <div class="modal" :class="{ 'is-active': showAddToPantryModal }">
          <div class="modal-background" @click="closeModal"></div>
          <div class="modal-card">
            <header class="modal-card-head">
              <p class="modal-card-title">
                <i class="fas fa-box-open"></i> Ajouter au garde-manger
              </p>
              <button class="delete" aria-label="close" @click="closeModal"></button>
            </header>
            <section class="modal-card-body">
              <div class="field">
                <label class="label">Produit</label>
                <div class="control">
                  <input class="input" type="text" v-model="pantryForm.name" readonly>
                </div>
              </div>
              <div class="columns">
                <div class="column">
                  <div class="field">
                    <label class="label">Quantite</label>
                    <div class="control">
                      <input class="input" type="number" v-model="pantryForm.quantity" min="0.01" step="0.01" required>
                    </div>
                  </div>
                </div>
                <div class="column">
                  <div class="field">
                    <label class="label">Unite</label>
                    <div class="control">
                      <div class="select is-fullwidth">
                        <select v-model="pantryForm.unit">
                          <option value="">-</option>
                          <option value="g">g</option>
                          <option value="kg">kg</option>
                          <option value="L">L</option>
                          <option value="unité">unité</option>
                        </select>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="field">
                <label class="label">Date de peremption</label>
                <div class="control">
                  <input class="input" type="date" v-model="pantryForm.expiryDate" required>
                </div>
              </div>
              <div class="field">
                <label class="label">Lieu de stockage</label>
                <div class="control">
                  <div class="select is-fullwidth">
                    <select v-model="pantryForm.storageType">
                      <option value="frigo">Frigo</option>
                      <option value="congelateur">Congelateur</option>
                      <option value="placard">Placard</option>
                      <option value="panier">Panier</option>
                    </select>
                  </div>
                </div>
              </div>
            </section>
            <footer class="modal-card-foot">
              <button class="button is-success" @click="addToPantry" :disabled="addingToPantry">
                <span class="icon"><i class="fas fa-plus"></i></span>
                <span>{{ addingToPantry ? 'Ajout...' : 'Ajouter au garde-manger' }}</span>
              </button>
              <button class="button" @click="closeModal">Annuler</button>
            </footer>
          </div>
        </div>
        <!-- Formulaire d'ajout -->
        <div class="add-form">
          <form @submit.prevent="addItem" class="columns is-vcentered">
            <div class="column">
              <div class="control has-icons-left">
                <input class="input is-large" type="text" v-model="newItem.name"
                       placeholder="Ex: Tomates, Lait, Pain..." required>
                <span class="icon is-left"><i class="fas fa-shopping-basket"></i></span>
              </div>
            </div>
            <div class="column is-2">
              <input class="input is-large" type="number" v-model="newItem.quantity" placeholder="Qte" min="1">
            </div>
            <div class="column is-2">
              <div class="select is-large is-fullwidth">
                <select v-model="newItem.unit">
                  <option value="">-</option>
                  <option value="g">g</option>
                  <option value="kg">kg</option>
                  <option value="L">L</option>
                  <option value="unité">unité</option>
                </select>
              </div>
            </div>
            <div class="column is-narrow">
              <button type="submit" class="button is-primary is-large" :disabled="adding">
                <span class="icon"><i class="fas fa-plus"></i></span>
                <span>Ajouter</span>
              </button>
            </div>
          </form>
        </div>

        <!-- Papier de liste -->
        <div class="paper-container">
          <div class="paper-header">
            <h1 class="paper-title">Liste de Courses</h1>
            <p class="paper-subtitle">{{ totalItems }} article(s)</p>
          </div>

          <div class="lined-paper">
            <!-- Liste vide -->
            <div v-if="manualItems.length === 0 && recipeIngredients.length === 0" class="empty-list">
              <i class="fas fa-clipboard-list"></i>
              <p>Liste vide</p>
              <p class="empty-subtitle">Ajoutez des articles manuellement</p>
            </div>

            <!-- Ingredients manquants des recettes -->
            <template v-if="recipeIngredients.length > 0">
              <div class="recipe-divider">
                <i class="fas fa-utensils"></i> Ingredients des recettes
              </div>
              <div v-for="(ing, index) in recipeIngredients" :key="'recipe-'+index"
                   class="shopping-list-item recipe-ingredient-item"
                   @click="openAddToPantryModal(ing)">
                <div class="check-symbol missing">&#9744;</div>
                <div class="item-content">
                  <span class="item-name">{{ ing.name }}</span>
                  <span class="item-quantity" v-if="ing.quantity">
                    ({{ ing.quantity }} {{ ing.unit || '' }})
                  </span>
                  <span class="item-status status-missing"> &rarr; a acheter</span>
                </div>
                <button class="add-to-pantry-btn" @click.stop="openAddToPantryModal(ing)" title="Ajouter au garde-manger">
                  <i class="fas fa-box-open"></i>
                </button>
              </div>
              <div class="recipe-separator"></div>
            </template>

            <!-- Articles manuels -->
            <template v-if="manualItems.length > 0">
              <div class="manual-divider">
                <i class="fas fa-plus-circle"></i> Articles manuels
              </div>
              <div v-for="item in manualItems" :key="item.id"
                   class="shopping-list-item" :class="{ 'checked': item.checked }">
                <input type="checkbox" class="checkbox-custom"
                       :checked="item.checked" @change="toggleItem(item)">
                <div class="item-content">
                  <span class="item-name">{{ item.name }}</span>
                  <span class="item-quantity" v-if="item.quantity">
                    ({{ item.quantity }} {{ item.unit || '' }})
                  </span>
                </div>
                <button class="delete-btn" @click="deleteItem(item.id)">
                  <i class="fas fa-times"></i>
                </button>
              </div>
            </template>
          </div>

          <div class="paper-footer" v-if="hasCheckedItems">
            <button class="button is-light" @click="clearChecked">
              <span class="icon"><i class="fas fa-broom"></i></span>
              <span>Nettoyer les articles coches</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import {
  saveAllShoppingItems,
  getAllShoppingItems,
  addShoppingItem as addShoppingItemToDB,
  toggleShoppingItem as toggleShoppingItemInDB,
  deleteShoppingItem as deleteShoppingItemFromDB,
  updateShoppingItemServerId,
  getAllRecipes
} from '@/services/db'
import * as syncService from '@/services/sync'

export default {
  name: 'ShoppingView',
  data() {
    return {
      manualItems: [],
      recipeIngredients: [],
      loading: true,
      adding: false,
      isOnline: navigator.onLine,
      newItem: {
        name: '',
        quantity: null,
        unit: ''
      },
      // Modal garde-manger
      showAddToPantryModal: false,
      addingToPantry: false,
      pantryForm: {
        name: '',
        quantity: 1,
        unit: '',
        expiryDate: '',
        storageType: 'frigo'
      },
      selectedIngredient: null
    }
  },
  computed: {
    totalItems() {
      return this.manualItems.length + this.recipeIngredients.length
    },
    hasCheckedItems() {
      return this.manualItems.some(item => item.checked)
    }
  },
  mounted() {
    window.addEventListener('online', this.handleOnline)
    window.addEventListener('offline', this.handleOffline)
    this.fetchShoppingList()
  },
  beforeUnmount() {
    window.removeEventListener('online', this.handleOnline)
    window.removeEventListener('offline', this.handleOffline)
  },
  methods: {
    handleOnline() {
      this.isOnline = true
      this.fetchShoppingList()
    },
    handleOffline() {
      this.isOnline = false
    },
    async getOfflineRecipeIngredients() {
      try {
        const recipes = await getAllRecipes()
        const ingredients = []
        for (const recipe of recipes) {
          if (recipe.missingIngredients && recipe.missingIngredients.length > 0) {
            for (const ing of recipe.missingIngredients) {
              ingredients.push({
                name: ing.name,
                quantity: ing.quantity,
                unit: ing.unit || '',
                fromRecipe: recipe.name,
                recipeId: recipe.recipeId
              })
            }
          }
        }
        return ingredients
      } catch (e) {
        console.warn('Erreur calcul ingredients hors ligne:', e)
        return []
      }
    },
    async fetchShoppingList() {
      this.loading = true
      try {
        if (this.isOnline) {
          const response = await fetch('/api/vue/shopping-list', { credentials: 'include' })
          if (response.ok) {
            const data = await response.json()
            this.manualItems = data.manualItems || []
            this.recipeIngredients = data.recipeIngredients || []
            // Sauvegarder en cache (en arriere-plan, ne pas bloquer)
            saveAllShoppingItems(this.manualItems).catch(err => {
              console.warn('Erreur sauvegarde cache shopping:', err)
            })
          } else {
            // API a echoue, charger depuis le cache
            this.manualItems = await getAllShoppingItems()
            this.recipeIngredients = []
          }
        } else {
          // Mode hors ligne
          this.manualItems = await getAllShoppingItems()
          // Calculer les ingredients manquants depuis les recettes en cache
          this.recipeIngredients = await this.getOfflineRecipeIngredients()
        }
      } catch (e) {
        console.error('Erreur chargement liste:', e)
        // En cas d'erreur reseau, charger depuis le cache
        try {
          this.manualItems = await getAllShoppingItems()
        } catch (cacheErr) {
          console.error('Erreur cache shopping:', cacheErr)
          this.manualItems = []
        }
        this.recipeIngredients = []
      } finally {
        this.loading = false
      }
    },
    async addItem() {
      if (!this.newItem.name.trim()) return
      this.adding = true

      try {
        if (this.isOnline) {
          const response = await fetch('/api/vue/shopping-list', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(this.newItem)
          })

          if (response.ok) {
            const item = await response.json()
            this.manualItems.unshift(item)
            const localItem = await addShoppingItemToDB({ ...this.newItem })
            await updateShoppingItemServerId(localItem.id, item.id)
            this.newItem = { name: '', quantity: null, unit: '' }
          }
        } else {
          const item = await addShoppingItemToDB({ ...this.newItem })
          this.manualItems.unshift(item)
          this.newItem = { name: '', quantity: null, unit: '' }
        }
      } catch (e) {
        console.error('Erreur ajout article:', e)
      } finally {
        this.adding = false
      }
    },
    async toggleItem(item) {
      try {
        if (this.isOnline) {
          const response = await fetch(`/api/vue/shopping-list/${item.id}/toggle`, {
            method: 'PUT',
            credentials: 'include'
          })

          if (response.ok) {
            item.checked = !item.checked
            await toggleShoppingItemInDB(item.id)
          }
        } else {
          await toggleShoppingItemInDB(item.id)
          item.checked = !item.checked
        }
      } catch (e) {
        console.error('Erreur toggle article:', e)
      }
    },
    async deleteItem(id) {
      try {
        if (this.isOnline) {
          const response = await fetch(`/api/vue/shopping-list/${id}`, {
            method: 'DELETE',
            credentials: 'include'
          })

          if (response.ok) {
            this.manualItems = this.manualItems.filter(item => item.id !== id)
            await deleteShoppingItemFromDB(id)
          }
        } else {
          await deleteShoppingItemFromDB(id)
          this.manualItems = this.manualItems.filter(item => item.id !== id)
        }
      } catch (e) {
        console.error('Erreur suppression article:', e)
      }
    },
    async clearChecked() {
      if (!confirm('Supprimer tous les articles coches ?')) return

      try {
        if (this.isOnline) {
          const response = await fetch('/api/vue/shopping-list/checked', {
            method: 'DELETE',
            credentials: 'include'
          })

          if (response.ok) {
            const checkedIds = this.manualItems.filter(item => item.checked).map(item => item.id)
            this.manualItems = this.manualItems.filter(item => !item.checked)
            for (const id of checkedIds) {
              await deleteShoppingItemFromDB(id)
            }
          }
        } else {
          const checkedIds = this.manualItems.filter(item => item.checked).map(item => item.id)
          for (const id of checkedIds) {
            await deleteShoppingItemFromDB(id)
          }
          this.manualItems = this.manualItems.filter(item => !item.checked)
        }
      } catch (e) {
        console.error('Erreur suppression coches:', e)
      }
    },
    // Methodes pour la modal garde-manger
    openAddToPantryModal(ingredient) {
      this.selectedIngredient = ingredient
      // Calculer une date par defaut (dans 7 jours)
      const defaultDate = new Date()
      defaultDate.setDate(defaultDate.getDate() + 7)
      const dateStr = defaultDate.toISOString().split('T')[0]

      this.pantryForm = {
        name: ingredient.name,
        quantity: ingredient.quantity || 1,
        unit: ingredient.unit || '',
        expiryDate: dateStr,
        storageType: 'frigo'
      }
      this.showAddToPantryModal = true
    },
    closeModal() {
      this.showAddToPantryModal = false
      this.selectedIngredient = null
    },
    async addToPantry() {
      if (!this.pantryForm.name || !this.pantryForm.expiryDate) {
        alert('Veuillez remplir tous les champs obligatoires')
        return
      }

      this.addingToPantry = true
      try {
        await syncService.addProduct({
          name: this.pantryForm.name,
          quantity: this.pantryForm.quantity,
          unit: this.pantryForm.unit,
          expiryDate: this.pantryForm.expiryDate,
          storageType: this.pantryForm.storageType
        })

        // Retirer l'ingredient de la liste des recettes (visuellement)
        if (this.selectedIngredient) {
          const idx = this.recipeIngredients.findIndex(
            ing => ing.name === this.selectedIngredient.name
          )
          if (idx !== -1) {
            this.recipeIngredients.splice(idx, 1)
          }
        }

        this.closeModal()
        alert('Produit ajoute au garde-manger !')
      } catch (e) {
        console.error('Erreur ajout au garde-manger:', e)
        alert('Erreur lors de l\'ajout au garde-manger')
      } finally {
        this.addingToPantry = false
      }
    }
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Indie+Flower&family=Permanent+Marker&display=swap');

.shopping-section {
  padding: 2.5rem 1.5rem;
  background: var(--bg-light);
}

/* Formulaire d'ajout */
.add-form {
  background: var(--bg-white, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 16px;
  padding: 2rem;
  margin-bottom: 2rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.add-form:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  border-color: rgba(209, 122, 70, 0.3);
}

.add-form .control.has-icons-left .icon {
  color: var(--eco-orange, #d17a46);
  opacity: 0.6;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.add-form .input {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.add-form .input:focus {
  outline: none;
  border-color: var(--eco-orange, #d17a46);
  box-shadow: 0 0 0 3px rgba(209, 122, 70, 0.1);
}

.add-form .button.is-primary {
  background: linear-gradient(135deg, var(--eco-green, #6b9440), var(--eco-orange, #d17a46));
  color: white;
  box-shadow: 0 4px 12px rgba(209, 122, 70, 0.25);
  border-radius: 12px;
  font-weight: 700;
  border: none;
  transition: all 0.3s ease;
}

.add-form .button.is-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(209, 122, 70, 0.35);
}

/* Conteneur papier */
.paper-container {
  max-width: 850px;
  margin: 2rem auto;
  background: linear-gradient(to bottom, #fdfcfb 0%, #f8f6f0 100%);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

.paper-container:hover {
  box-shadow: 0 12px 35px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

/* En-tete papier */
.paper-header {
  background: linear-gradient(135deg, var(--eco-orange, #d17a46) 0%, #b86838 100%);
  padding: 2.5rem 2rem;
  text-align: center;
  border-bottom: 4px solid var(--eco-green, #6b9440);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.paper-title {
  font-family: 'Permanent Marker', cursive;
  color: white;
  font-size: 2.75rem;
  margin: 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  letter-spacing: 0.05em;
}

.paper-subtitle {
  color: rgba(255, 255, 255, 0.95);
  margin-top: 0.75rem;
  font-size: 1.15rem;
  font-weight: 600;
}

/* Zone lignee */
.lined-paper {
  background-image:
    linear-gradient(transparent, transparent 39px, rgba(203, 213, 225, 0.3) 39px, rgba(203, 213, 225, 0.3) 40px);
  background-size: 100% 40px;
  padding: 2.5rem 1.5rem 2.5rem 4.5rem;
  min-height: 400px;
  position: relative;
}

.lined-paper::before {
  content: '';
  position: absolute;
  left: 4rem;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, var(--eco-orange, #d17a46) 0%, #b86838 100%);
  box-shadow: 1px 0 2px rgba(209, 122, 70, 0.2);
}

/* Trous de perforation */
.lined-paper::after {
  content: '';
  position: absolute;
  left: 2.2rem;
  top: 2rem;
  bottom: 2rem;
  width: 8px;
  background-image: radial-gradient(circle, rgba(107, 148, 64, 0.15) 35%, transparent 35%);
  background-size: 8px 40px;
  background-position: 0 0;
}

/* Item de liste */
.shopping-list-item {
  display: flex;
  align-items: center;
  height: 40px;
  padding-right: 1.5rem;
  transition: all 0.2s ease;
  border-radius: 8px;
  animation: fadeIn 0.3s ease;
}

.shopping-list-item:hover {
  background: rgba(107, 148, 64, 0.05);
  margin-left: -0.5rem;
  padding-left: 0.5rem;
}

.shopping-list-item.checked .item-name {
  text-decoration: line-through;
  text-decoration-thickness: 2px;
  opacity: 0.4;
  color: var(--light-text, #9ca3af);
}

/* Checkbox style */
.checkbox-custom {
  width: 26px;
  height: 26px;
  cursor: pointer;
  margin-right: 1rem;
  accent-color: var(--eco-green, #6b9440);
  border-radius: 6px;
  transition: transform 0.2s ease;
}

.checkbox-custom:hover {
  transform: scale(1.1);
}

/* Contenu item */
.item-content {
  flex: 1;
  font-family: 'Indie Flower', cursive;
  font-size: 1.45rem;
  color: var(--dark-text, #374151);
  line-height: 1.2;
}

.item-name {
  font-weight: 600;
}

.item-quantity {
  color: var(--eco-orange, #d17a46);
  font-weight: bold;
  margin-left: 0.5rem;
}

/* Symbole de check */
.check-symbol {
  width: 28px;
  margin-right: 1rem;
  font-size: 1.2rem;
  font-weight: bold;
  text-align: center;
}

.check-symbol.missing {
  color: var(--danger-dark, #dc2626);
}

/* Status */
.item-status {
  font-family: 'Inter', sans-serif;
  font-size: 0.9rem;
  font-weight: 600;
  margin-left: 0.75rem;
}

.status-missing {
  color: var(--danger-dark, #dc2626);
}

/* Bouton supprimer */
.delete-btn {
  background: transparent;
  border: none;
  color: var(--danger-dark, #dc2626);
  cursor: pointer;
  opacity: 0.3;
  transition: all 0.3s ease;
  padding: 0.5rem;
  border-radius: 6px;
  font-size: 1.1rem;
}

.delete-btn:hover {
  opacity: 1;
  background: rgba(220, 38, 38, 0.1);
  transform: scale(1.15);
}

/* Diviseur recette */
.recipe-divider {
  display: flex;
  align-items: center;
  height: 40px;
  font-family: 'Indie Flower', cursive;
  font-weight: bold;
  font-size: 1.5rem;
  color: var(--eco-orange, #d17a46);
  text-decoration: underline;
  text-decoration-color: var(--eco-orange, #d17a46);
  text-decoration-thickness: 2px;
  text-underline-offset: 4px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.recipe-divider i {
  margin-right: 0.5rem;
  font-size: 1.3rem;
}

/* Diviseur manuel */
.manual-divider {
  display: flex;
  align-items: center;
  height: 40px;
  font-family: 'Indie Flower', cursive;
  font-weight: bold;
  font-size: 1.5rem;
  color: var(--eco-green, #6b9440);
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 2px dashed rgba(107, 148, 64, 0.4);
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.manual-divider i {
  margin-right: 0.5rem;
  font-size: 1.3rem;
}

.recipe-separator {
  height: 40px;
}

/* Items cliquables des recettes */
.recipe-ingredient-item {
  cursor: pointer;
  position: relative;
}

.recipe-ingredient-item:hover {
  background: rgba(209, 122, 70, 0.1) !important;
}

.recipe-ingredient-item:hover .add-to-pantry-btn {
  opacity: 1;
}

.add-to-pantry-btn {
  background: linear-gradient(135deg, var(--eco-green, #6b9440), var(--eco-orange, #d17a46));
  border: none;
  color: white;
  cursor: pointer;
  opacity: 0.3;
  transition: all 0.3s ease;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  font-size: 1rem;
  margin-left: auto;
}

.add-to-pantry-btn:hover {
  opacity: 1;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(107, 148, 64, 0.3);
}

/* Modal styles */
.modal-card-head {
  background: linear-gradient(135deg, var(--eco-orange, #d17a46) 0%, #b86838 100%);
}

.modal-card-title {
  color: white;
  font-weight: 700;
}

.modal-card-title i {
  margin-right: 0.5rem;
}

.modal-card-head .delete {
  background: rgba(255, 255, 255, 0.3);
}

.modal-card-head .delete:hover {
  background: rgba(255, 255, 255, 0.5);
}

.modal-card-foot {
  background: var(--bg-light, #f9fafb);
  justify-content: flex-end;
  gap: 0.5rem;
}

.modal-card-foot .button.is-success {
  background: linear-gradient(135deg, var(--eco-green, #6b9440), var(--eco-orange, #d17a46));
  border: none;
  font-weight: 600;
}

.modal-card-foot .button.is-success:hover {
  box-shadow: 0 4px 12px rgba(107, 148, 64, 0.3);
}

/* Liste vide */
.empty-list {
  text-align: center;
  padding: 4rem 2rem;
  font-family: 'Indie Flower', cursive;
  font-size: 1.5rem;
  color: var(--light-text, #9ca3af);
}

.empty-list i {
  font-size: 5rem;
  color: var(--border-color, #e5e7eb);
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-list .empty-subtitle {
  font-size: 1.1rem;
  color: var(--light-text, #9ca3af);
}

/* Footer papier */
.paper-footer {
  background: linear-gradient(180deg, #e8e4d9 0%, #d9d5ca 100%);
  padding: 1.5rem 2rem;
  text-align: center;
  border-top: 1px solid var(--border-color, #e5e7eb);
}

.paper-footer .button.is-light {
  background: var(--bg-white, #fff);
  color: var(--dark-text, #374151);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.paper-footer .button.is-light:hover {
  background: rgba(220, 38, 38, 0.1);
  border-color: var(--danger-dark, #dc2626);
  color: var(--danger-dark, #dc2626);
  transform: translateY(-2px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Responsive */
@media (max-width: 768px) {
  .shopping-section {
    padding: 2rem 1rem;
  }

  .add-form {
    padding: 1.5rem;
    border-radius: 12px;
  }

  .add-form .columns {
    display: block;
  }

  .add-form .column {
    width: 100%;
    margin-bottom: 1rem;
  }

  .add-form .button {
    width: 100%;
    justify-content: center;
  }

  .paper-container {
    margin: 1rem 0;
    border-radius: 12px;
  }

  .paper-header {
    padding: 2rem 1.5rem;
  }

  .paper-title {
    font-size: 2.25rem;
  }

  .lined-paper {
    padding: 2rem 1rem 2rem 3.5rem;
  }

  .lined-paper::before {
    left: 3rem;
  }

  .lined-paper::after {
    left: 1.7rem;
  }

  .item-content {
    font-size: 1.25rem;
  }

  .recipe-divider,
  .manual-divider {
    font-size: 1.3rem;
  }
}

@media (max-width: 480px) {
  .paper-title {
    font-size: 2rem;
  }

  .lined-paper {
    padding: 1.5rem 0.5rem 1.5rem 3rem;
  }

  .lined-paper::before {
    left: 2.5rem;
    width: 2px;
  }

  .lined-paper::after {
    left: 1.2rem;
  }

  .item-content {
    font-size: 1.15rem;
  }

  .item-quantity {
    display: block;
    margin-left: 0;
    font-size: 0.95rem;
  }

  .item-status {
    display: block;
    margin-left: 0;
    font-size: 0.85rem;
  }

  .checkbox-custom {
    width: 22px;
    height: 22px;
  }

  .check-symbol {
    width: 24px;
    font-size: 1rem;
  }
}
</style>
