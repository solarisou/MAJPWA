import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  base: '/vue/',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/pantry/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/scanner/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
