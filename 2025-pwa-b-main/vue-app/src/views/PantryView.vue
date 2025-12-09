<template>
  <section class="section">
    <div class="container">
      <!-- Header -->
      <div class="level mb-5">
        <div class="level-left">
          <div class="level-item">
            <div>
              <h1 class="title is-2">
                <span class="icon"><i class="fas fa-box-open"></i></span>
                Mon Garde-manger
              </h1>
              <p class="subtitle">Gerez vos produits et reduisez le gaspillage</p>
            </div>
          </div>
        </div>
        <div class="level-right">
          <div class="level-item">
            <span class="tag is-primary is-large">
              <i class="fas fa-shopping-basket mr-2"></i>
              {{ total }} produit(s)
            </span>
          </div>
        </div>
      </div>

      <!-- Indicateur de sync -->
      <div v-if="pendingSync > 0" class="notification is-warning is-light mb-4">
        <span class="icon"><i class="fas fa-sync-alt fa-spin"></i></span>
        <span>{{ pendingSync }} modification(s) en attente de synchronisation</span>
      </div>

      <!-- Message d'erreur -->
      <div v-if="error" class="notification is-danger is-light">
        <button class="delete" @click="error = null"></button>
        {{ error }}
      </div>

      <!-- Message de succes -->
      <div v-if="success" class="notification is-success is-light">
        <button class="delete" @click="success = null"></button>
        {{ success }}
      </div>

      <!-- Loading -->
      <div v-if="loading" class="has-text-centered" style="padding: 4rem 0;">
        <span class="icon is-large">
          <i class="fas fa-spinner fa-spin fa-3x" style="color: var(--eco-green);"></i>
        </span>
        <p class="mt-4" style="color: var(--light-text);">Chargement de votre garde-manger...</p>
      </div>

      <!-- Formulaire d'ajout -->
      <div class="box mb-5">
        <h2 class="title is-4">
          <i class="fas fa-plus-circle" style="color: var(--eco-orange);"></i>
          Ajouter un produit
        </h2>
        <form @submit.prevent="handleAddProduct">
          <div class="columns is-multiline">
            <div class="column is-4">
              <div class="field">
                <label class="label">Nom du produit</label>
                <div class="control has-icons-left">
                  <input class="input" type="text" v-model="newProduct.name" placeholder="Ex: Lait, Oeufs, Pain..." required>
                  <span class="icon is-left"><i class="fas fa-tag"></i></span>
                </div>
              </div>
            </div>
            <div class="column is-2">
              <div class="field">
                <label class="label">Quantite</label>
                <div class="control">
                  <input class="input" type="number" v-model="newProduct.quantity" min="1" required>
                </div>
              </div>
            </div>
            <div class="column is-2">
              <div class="field">
                <label class="label">Unite</label>
                <div class="control">
                  <div class="select is-fullwidth">
                    <select v-model="newProduct.unit">
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
            <div class="column is-2">
              <div class="field">
                <label class="label">Peremption</label>
                <div class="control">
                  <input class="input" type="date" v-model="newProduct.expiryDate" required>
                </div>
              </div>
            </div>
            <div class="column is-2">
              <div class="field">
                <label class="label">Stockage</label>
                <div class="control">
                  <div class="select is-fullwidth">
                    <select v-model="newProduct.storageType">
                      <option value="frigo">Frigo</option>
                      <option value="congelateur">Congelateur</option>
                      <option value="placard">Placard</option>
                      <option value="panier">Panier</option>
                    </select>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <button type="submit" class="button is-primary" :disabled="adding">
            <span class="icon"><i class="fas fa-plus"></i></span>
            <span>{{ adding ? 'Ajout en cours...' : 'Ajouter' }}</span>
          </button>
        </form>
      </div>

      <!-- Storage Grid -->
      <div v-if="!loading" class="storage-grid">
        <!-- Frigo -->
        <div class="storage-box frigo" :class="{ 'expanded': expandedBox === 'frigo' }" @click="toggleBox('frigo')">
          <div class="storage-content">
            <h3 class="storage-title"><i class="fas fa-snowflake"></i> Frigo</h3>
            <span class="storage-count">{{ frigo.length }}</span>
          </div>
          <div v-if="frigo.length > 0" class="product-list">
            <div v-for="product in frigo" :key="product.id" class="product-item" :class="{ 'pending': product.syncStatus === 'pending' }">
              <div>
                <span class="product-name">{{ product.name }}</span>
                <span class="product-qty">{{ product.quantity }} {{ product.unit || 'unite(s)' }}</span>
                <span v-if="product.syncStatus === 'pending'" class="status-badge" style="background: #e0e7ff; color: #4f46e5;"><i class="fas fa-clock"></i></span>
                <span v-if="isExpired(product)" class="status-badge status-danger">Expire</span>
                <span v-else-if="isExpiringSoon(product)" class="status-badge status-warning">Bientot</span>
                <span v-else class="status-badge status-ok">OK</span>
              </div>
              <div>
                <span class="product-date">{{ formatDate(product.expiryDate) }}</span>
                <button class="delete-btn" @click.stop="handleDeleteProduct(product)"><i class="fas fa-trash"></i></button>
              </div>
            </div>
          </div>
          <div v-else class="empty-storage">Aucun produit</div>
        </div>

        <!-- Congelateur -->
        <div class="storage-box congelateur" :class="{ 'expanded': expandedBox === 'congelateur' }" @click="toggleBox('congelateur')">
          <div class="storage-content">
            <h3 class="storage-title"><i class="fas fa-ice-cream"></i> Congelateur</h3>
            <span class="storage-count">{{ congelateur.length }}</span>
          </div>
          <div v-if="congelateur.length > 0" class="product-list">
            <div v-for="product in congelateur" :key="product.id" class="product-item" :class="{ 'pending': product.syncStatus === 'pending' }">
              <div>
                <span class="product-name">{{ product.name }}</span>
                <span class="product-qty">{{ product.quantity }} {{ product.unit || 'unite(s)' }}</span>
                <span v-if="product.syncStatus === 'pending'" class="status-badge" style="background: #e0e7ff; color: #4f46e5;"><i class="fas fa-clock"></i></span>
                <span v-if="isExpired(product)" class="status-badge status-danger">Expire</span>
                <span v-else-if="isExpiringSoon(product)" class="status-badge status-warning">Bientot</span>
                <span v-else class="status-badge status-ok">OK</span>
              </div>
              <div>
                <span class="product-date">{{ formatDate(product.expiryDate) }}</span>
                <button class="delete-btn" @click.stop="handleDeleteProduct(product)"><i class="fas fa-trash"></i></button>
              </div>
            </div>
          </div>
          <div v-else class="empty-storage">Aucun produit</div>
        </div>

        <!-- Placard -->
        <div class="storage-box placard" :class="{ 'expanded': expandedBox === 'placard' }" @click="toggleBox('placard')">
          <div class="storage-content">
            <h3 class="storage-title"><i class="fas fa-box"></i> Placard</h3>
            <span class="storage-count">{{ placard.length }}</span>
          </div>
          <div v-if="placard.length > 0" class="product-list">
            <div v-for="product in placard" :key="product.id" class="product-item" :class="{ 'pending': product.syncStatus === 'pending' }">
              <div>
                <span class="product-name">{{ product.name }}</span>
                <span class="product-qty">{{ product.quantity }} {{ product.unit || 'unite(s)' }}</span>
                <span v-if="product.syncStatus === 'pending'" class="status-badge" style="background: #e0e7ff; color: #4f46e5;"><i class="fas fa-clock"></i></span>
                <span v-if="isExpired(product)" class="status-badge status-danger">Expire</span>
                <span v-else-if="isExpiringSoon(product)" class="status-badge status-warning">Bientot</span>
                <span v-else class="status-badge status-ok">OK</span>
              </div>
              <div>
                <span class="product-date">{{ formatDate(product.expiryDate) }}</span>
                <button class="delete-btn" @click.stop="handleDeleteProduct(product)"><i class="fas fa-trash"></i></button>
              </div>
            </div>
          </div>
          <div v-else class="empty-storage">Aucun produit</div>
        </div>

        <!-- Panier -->
        <div class="storage-box panier" :class="{ 'expanded': expandedBox === 'panier' }" @click="toggleBox('panier')">
          <div class="storage-content">
            <h3 class="storage-title"><i class="fas fa-shopping-basket"></i> Panier</h3>
            <span class="storage-count">{{ panier.length }}</span>
          </div>
          <div v-if="panier.length > 0" class="product-list">
            <div v-for="product in panier" :key="product.id" class="product-item" :class="{ 'pending': product.syncStatus === 'pending' }">
              <div>
                <span class="product-name">{{ product.name }}</span>
                <span class="product-qty">{{ product.quantity }} {{ product.unit || 'unite(s)' }}</span>
                <span v-if="product.syncStatus === 'pending'" class="status-badge" style="background: #e0e7ff; color: #4f46e5;"><i class="fas fa-clock"></i></span>
                <span v-if="isExpired(product)" class="status-badge status-danger">Expire</span>
                <span v-else-if="isExpiringSoon(product)" class="status-badge status-warning">Bientot</span>
                <span v-else class="status-badge status-ok">OK</span>
              </div>
              <div>
                <span class="product-date">{{ formatDate(product.expiryDate) }}</span>
                <button class="delete-btn" @click.stop="handleDeleteProduct(product)"><i class="fas fa-trash"></i></button>
              </div>
            </div>
          </div>
          <div v-else class="empty-storage">Aucun produit</div>
        </div>
      </div>
    </div>
  </section>
</template>

<script>
import * as syncService from '../services/sync.js'

export default {
  name: 'PantryView',
  data() {
    return {
      frigo: [],
      congelateur: [],
      placard: [],
      panier: [],
      total: 0,
      loading: true,
      adding: false,
      error: null,
      success: null,
      expandedBox: null,
      pendingSync: 0,
      newProduct: {
        name: '',
        quantity: 1,
        unit: '',
        expiryDate: '',
        storageType: 'placard'
      }
    }
  },
  mounted() {
    this.fetchProducts()
    // Configurer les listeners de connectivite
    syncService.setupConnectivityListeners(
      () => this.onOnline(),
      () => this.onOffline()
    )
    // Verifier les syncs en attente
    this.updatePendingCount()
  },
  methods: {
    toggleBox(boxName) {
      this.expandedBox = this.expandedBox === boxName ? null : boxName
    },
    formatDate(dateStr) {
      if (!dateStr) return ''
      return new Date(dateStr).toLocaleDateString('fr-FR')
    },
    isExpired(product) {
      if (!product.expiryDate) return false
      return new Date(product.expiryDate) < new Date()
    },
    isExpiringSoon(product) {
      if (!product.expiryDate) return false
      const expiry = new Date(product.expiryDate)
      const now = new Date()
      const inSevenDays = new Date()
      inSevenDays.setDate(now.getDate() + 7)
      return expiry > now && expiry <= inSevenDays
    },
    async fetchProducts() {
      this.loading = true
      try {
        const products = await syncService.getProductsGrouped()
        this.frigo = products.frigo || []
        this.congelateur = products.congelateur || []
        this.placard = products.placard || []
        this.panier = products.panier || []
        this.total = this.frigo.length + this.congelateur.length + this.placard.length + this.panier.length
        await this.updatePendingCount()
      } catch (e) {
        this.error = 'Impossible de charger les produits.'
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    async handleAddProduct() {
      this.adding = true
      this.error = null
      try {
        await syncService.addProduct({ ...this.newProduct })
        this.success = navigator.onLine ? 'Produit ajoute !' : 'Produit ajoute localement (sync quand en ligne)'
        this.newProduct = { name: '', quantity: 1, unit: '', expiryDate: '', storageType: 'placard' }
        await this.fetchProducts()
        setTimeout(() => this.success = null, 3000)
      } catch (e) {
        this.error = 'Erreur lors de l\'ajout'
        console.error(e)
      } finally {
        this.adding = false
      }
    },
    async handleDeleteProduct(product) {
      if (!confirm('Supprimer ce produit ?')) return
      try {
        await syncService.deleteProduct(product.id, product.serverId)
        this.success = 'Produit supprime !'
        await this.fetchProducts()
        setTimeout(() => this.success = null, 3000)
      } catch (e) {
        this.error = 'Erreur lors de la suppression'
        console.error(e)
      }
    },
    async updatePendingCount() {
      this.pendingSync = await syncService.getPendingCount()
    },
    async onOnline() {
      this.success = 'Connexion retablie - synchronisation en cours...'
      await this.fetchProducts()
      await this.updatePendingCount()
      setTimeout(() => this.success = null, 3000)
    },
    onOffline() {
      this.error = 'Vous etes hors ligne. Les modifications seront synchronisees automatiquement.'
      setTimeout(() => this.error = null, 5000)
    }
  }
}
</script>

<style scoped>
.product-item.pending {
  opacity: 0.8;
  border-left: 3px solid #4f46e5;
}
</style>
