import { createApp } from 'vue'
import '@fontsource/alegreya-sans/latin-400.css'
import '@fontsource/alegreya-sans/latin-500.css'
import '@fontsource/alegreya-sans/latin-700.css'
import '@fontsource/alegreya-sc/latin-500.css'
import '@fontsource/alegreya-sc/latin-700.css'
import '@fontsource/alegreya/latin-400.css'
import '@fontsource/alegreya/latin-400-italic.css'
import '@fontsource/alegreya/latin-700.css'
import '@fontsource/alegreya/latin-700-italic.css'
import App from './App.vue'
import { router } from './router'
import { applyTheme, theme } from './theme'
import './style.css'

applyTheme(theme.value)
createApp(App).use(router).mount('#app')
