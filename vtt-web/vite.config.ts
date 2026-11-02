import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Le serveur Quarkus (vtt-server) écoute sur 8080 ; le proxy évite d'ouvrir le CORS.
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
})
