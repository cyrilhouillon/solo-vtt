export type Theme = 'dark' | 'light'

const KEY = 'solo-vtt.theme'

/** Le thème choisi par le joueur, sinon celui du système. */
export function initialTheme(): Theme {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved === 'dark' || saved === 'light') return saved
  } catch {
    // Stockage indisponible : on suit le système.
  }
  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark'
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
}

export function saveTheme(theme: Theme): void {
  applyTheme(theme)
  try {
    localStorage.setItem(KEY, theme)
  } catch {
    // Le choix vaut pour cette page seulement.
  }
}
