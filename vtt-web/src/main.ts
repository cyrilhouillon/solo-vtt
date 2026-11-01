import { createApp } from 'vue'
import '@fontsource/alegreya-sans/latin-400.css'
import '@fontsource/alegreya-sans/latin-500.css'
import '@fontsource/alegreya-sans/latin-700.css'
import '@fontsource/alegreya-sc/latin-500.css'
import '@fontsource/alegreya-sc/latin-700.css'
import App from './App.vue'
import { applyTheme, initialTheme } from './theme'
import './style.css'

applyTheme(initialTheme())
createApp(App).mount('#app')
