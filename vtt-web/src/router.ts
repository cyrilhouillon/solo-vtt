import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { api } from './api'
import GameScreen from './components/GameScreen.vue'
import HomeScreen from './components/HomeScreen.vue'
import NotebookScreen from './components/NotebookScreen.vue'
import type { CampaignView } from './types'

/** Les campagnes déjà lues, par id : une campagne n'est jamais supprimée, son nom suffit à l'écran. */
const campaigns = new Map<string, CampaignView>()

const campaignId = (route: RouteLocationNormalized) => route.params.campaignId as string

/**
 * Les écrans de l'application. L'adresse dit où est le joueur : un rechargement ou un favori la rouvre, Précédent et
 * Suivant la parcourent. Chaque écran relit sa campagne au serveur en se montant.
 */
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeScreen },
    {
      path: '/campagnes/:campaignId/carnet/:pageId?',
      name: 'notebook',
      component: NotebookScreen,
      props: (route) => ({ campaign: campaigns.get(campaignId(route)), pageId: (route.params.pageId as string | undefined) || null }),
    },
    {
      path: '/campagnes/:campaignId/combat',
      name: 'combat',
      component: GameScreen,
      props: (route) => ({ campaign: campaigns.get(campaignId(route)) }),
    },
    {
      path: '/campagnes/:campaignId/combats/:combatId/rejeu',
      name: 'combatReplay',
      component: GameScreen,
      props: (route) => ({ campaign: campaigns.get(campaignId(route)), replayId: route.params.combatId as string }),
    },
    // Une adresse inconnue ne casse rien : elle mène à l'accueil.
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

/** La campagne d'une adresse est lue avant d'entrer ; inconnue, ou serveur injoignable, l'adresse mène à l'accueil. */
router.beforeEach(async (to) => {
  if (!to.params.campaignId) return true
  const id = campaignId(to)
  if (campaigns.has(id)) return true
  try {
    for (const campaign of await api.campaigns()) campaigns.set(campaign.id, campaign)
  } catch {
    return { name: 'home', replace: true }
  }
  return campaigns.has(id) ? true : { name: 'home', replace: true }
})
