import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PantryView from '../views/PantryView.vue'
import RecipesView from '../views/RecipesView.vue'
import RecipeDetailView from '../views/RecipeDetailView.vue'
import ShoppingView from '../views/ShoppingView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/pantry',
    name: 'pantry',
    component: PantryView
  },
  {
    path: '/recipes',
    name: 'recipes',
    component: RecipesView
  },
  {
    path: '/recipes/:id',
    name: 'recipe-detail',
    component: RecipeDetailView
  },
  {
    path: '/shopping',
    name: 'shopping',
    component: ShoppingView
  }
]

const router = createRouter({
  history: createWebHistory('/vue/'),
  routes
})

export default router
