<template>
  <section class="section">
    <div class="container">
      <!-- Hero Section -->
      <div class="hero-content has-text-centered" style="padding: 2rem 0;">
        <span class="app-logo">ECO Cook</span>
        <p class="subtitle is-5 mt-3" v-if="user">
          Bienvenue, <strong>{{ user.displayName || user.userName }}</strong>
        </p>
        <p class="subtitle is-5 mt-3" v-else>Votre garde-manger intelligent</p>
      </div>

      <!-- Actions rapides -->
      <div class="quick-actions mt-5">
        <router-link to="/pantry" class="action-card">
          <span class="action-icon" style="background: linear-gradient(135deg, #22c55e, #16a34a);">
            <i class="fas fa-box-open"></i>
          </span>
          <span class="action-label">Garde-manger</span>
          <span class="action-count">{{ stats.total }} produits</span>
        </router-link>

        <router-link to="/recipes" class="action-card">
          <span class="action-icon" style="background: linear-gradient(135deg, #f97316, #ea580c);">
            <i class="fas fa-utensils"></i>
          </span>
          <span class="action-label">Recettes</span>
          <span class="action-count">{{ stats.selectedRecipes }} selectionnees</span>
        </router-link>

        <router-link to="/shopping" class="action-card">
          <span class="action-icon" style="background: linear-gradient(135deg, #3b82f6, #2563eb);">
            <i class="fas fa-shopping-cart"></i>
          </span>
          <span class="action-label">Courses</span>
          <span class="action-count">{{ stats.shoppingList }} articles</span>
        </router-link>
      </div>

      <!-- Alertes produits -->
      <div class="columns is-centered mt-6">
        <div class="column is-10">
          <div class="box">
            <h3 class="title is-5 mb-4">
              <i class="fas fa-bell" style="color: var(--eco-orange);"></i>
              Alertes
            </h3>
            <div class="columns is-mobile has-text-centered">
              <div class="column">
                <div class="alert-card alert-ok">
                  <i class="fas fa-check-circle"></i>
                  <span class="alert-count">{{ stats.ok }}</span>
                  <span class="alert-label">OK</span>
                </div>
              </div>
              <div class="column">
                <div class="alert-card alert-warning">
                  <i class="fas fa-exclamation-triangle"></i>
                  <span class="alert-count">{{ stats.expiringSoon }}</span>
                  <span class="alert-label">Bientot</span>
                </div>
              </div>
              <div class="column">
                <div class="alert-card alert-danger">
                  <i class="fas fa-times-circle"></i>
                  <span class="alert-count">{{ stats.expired }}</span>
                  <span class="alert-label">Expires</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Info PWA -->
      <div class="has-text-centered mt-5" style="color: var(--light-text);">
        <p><i class="fas fa-mobile-alt"></i> Application PWA</p>
        <p class="is-size-7 mt-1">Fonctionne meme hors connexion</p>
      </div>
    </div>
  </section>
</template>

<script>
export default {
  name: 'HomeView',
  props: ['user'],
  data() {
    return {
      stats: {
        total: 0,
        expiringSoon: 0,
        expired: 0,
        ok: 0,
        selectedRecipes: 0,
        shoppingList: 0
      }
    }
  },
  mounted() {
    this.fetchStats()
  },
  methods: {
    async fetchStats() {
      try {
        // Stats produits
        const productsResponse = await fetch('/pantry/api/products', { credentials: 'include' })
        if (productsResponse.ok) {
          const data = await productsResponse.json()
          const allProducts = [...(data.frigo || []), ...(data.congelateur || []), ...(data.placard || []), ...(data.panier || [])]
          const now = new Date()
          const inSevenDays = new Date()
          inSevenDays.setDate(now.getDate() + 7)

          this.stats.total = allProducts.length
          this.stats.expired = allProducts.filter(p => p.expiryDate && new Date(p.expiryDate) < now).length
          this.stats.expiringSoon = allProducts.filter(p => {
            if (!p.expiryDate) return false
            const exp = new Date(p.expiryDate)
            return exp > now && exp <= inSevenDays
          }).length
          this.stats.ok = this.stats.total - this.stats.expired - this.stats.expiringSoon
        }

        // Stats utilisateur
        const userResponse = await fetch('/api/vue/user', { credentials: 'include' })
        if (userResponse.ok) {
          const userData = await userResponse.json()
          this.stats.selectedRecipes = userData.selectedRecipesCount || 0
          this.stats.shoppingList = userData.shoppingListCount || 0
        }
      } catch (e) {
        console.log('Erreur chargement stats:', e)
      }
    }
  }
}
</script>

<style scoped>
.app-logo {
  font-weight: 800;
  font-size: 3rem;
  background: linear-gradient(135deg, var(--eco-green), var(--eco-orange));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: block;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 1rem;
  max-width: 500px;
  margin: 0 auto;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1.5rem 1rem;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 1.5rem;
  margin-bottom: 0.75rem;
}

.action-label {
  font-weight: 600;
  color: var(--dark-text);
  font-size: 0.95rem;
}

.action-count {
  font-size: 0.8rem;
  color: var(--light-text);
  margin-top: 0.25rem;
}

.alert-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  border-radius: 12px;
}

.alert-card i {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.alert-count {
  font-size: 1.75rem;
  font-weight: 700;
}

.alert-label {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.alert-ok {
  background: #dcfce7;
  color: #16a34a;
}

.alert-warning {
  background: #fef3c7;
  color: #d97706;
}

.alert-danger {
  background: #fee2e2;
  color: #dc2626;
}
</style>
