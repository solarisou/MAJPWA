<template>
  <div id="app">
    <!-- Navbar -->
    <nav class="navbar" role="navigation">
      <div class="container">
        <div class="navbar-brand">
          <router-link class="navbar-item logo-link" to="/">
            <span class="navbar-logo-text">ECO Cook</span>
          </router-link>

          <a role="button" class="navbar-burger" aria-label="menu" aria-expanded="false" @click="toggleMenu">
            <span aria-hidden="true"></span>
            <span aria-hidden="true"></span>
            <span aria-hidden="true"></span>
          </a>
        </div>

        <div class="navbar-menu" :class="{ 'is-active': menuActive }">
          <div class="navbar-start">
            <router-link class="navbar-item nav-link" to="/" :class="{ 'is-active': $route.path === '/' }">
              <i class="fas fa-home"></i>
              <span>Accueil</span>
            </router-link>

            <router-link class="navbar-item nav-link" to="/pantry" :class="{ 'is-active': $route.path === '/pantry' }">
              <i class="fas fa-box"></i>
              <span>Garde-manger</span>
            </router-link>

            <router-link class="navbar-item nav-link" to="/recipes" :class="{ 'is-active': $route.path === '/recipes' }">
              <i class="fas fa-utensils"></i>
              <span>Recettes</span>
            </router-link>

            <router-link class="navbar-item nav-link" to="/shopping" :class="{ 'is-active': $route.path === '/shopping' }">
              <i class="fas fa-shopping-cart"></i>
              <span>Courses</span>
            </router-link>
          </div>

          <div class="navbar-end">
            <div class="navbar-item">
              <span class="tag is-success is-light" v-if="isOnline">
                <i class="fas fa-wifi mr-1"></i> En ligne
              </span>
              <span class="tag is-warning is-light" v-else>
                <i class="fas fa-plane mr-1"></i> Hors ligne
              </span>
            </div>
            <div class="navbar-item" v-if="user">
              <div class="user-badge">
                <i class="fas fa-user-circle"></i>
                <span>{{ user.displayName || user.userName }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- Router View -->
    <router-view :user="user" />

    <!-- Footer -->
    <footer class="footer">
      <div class="container">
        <div class="footer-bottom has-text-centered">
          <p><strong>ECO Cook</strong> - Application PWA</p>
          <p class="footer-credits">Anti-gaspillage alimentaire</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      menuActive: false,
      isOnline: navigator.onLine,
      user: null
    }
  },
  mounted() {
    window.addEventListener('online', () => this.isOnline = true)
    window.addEventListener('offline', () => this.isOnline = false)
    this.fetchUser()
  },
  methods: {
    toggleMenu() {
      this.menuActive = !this.menuActive
    },
    async fetchUser() {
      try {
        const response = await fetch('/api/vue/user', { credentials: 'include' })
        if (response.ok) {
          this.user = await response.json()
        }
      } catch (e) {
        console.log('Erreur chargement utilisateur:', e)
      }
    }
  }
}
</script>

<style>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.navbar-logo-text {
  font-weight: 800;
  font-size: 1.5rem;
  background: linear-gradient(135deg, var(--eco-green), var(--eco-orange));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--bg-light);
  border-radius: 20px;
  font-weight: 500;
  color: var(--eco-green);
}

.user-badge i {
  font-size: 1.2rem;
}

.footer {
  margin-top: auto;
  padding: 1.5rem;
  background: var(--bg-light);
}

.footer-bottom {
  color: var(--light-text);
}

.footer-credits {
  font-size: 0.85rem;
  margin-top: 0.25rem;
}
</style>
