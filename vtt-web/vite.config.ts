import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Le serveur Quarkus (vtt-server) écoute sur 8080 ; le proxy évite d'ouvrir le CORS.
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
