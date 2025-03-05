import { createRouter, createWebHistory } from 'vue-router'
import Upload from './components/Upload.vue'
import ParsedEmail from './components/ParsedEmail.vue'
import Emails from './components/Emails.vue'
import Analytics from './components/Analytics.vue'

const routes = [
    { path: '/upload', component: Upload },
    { path: '/parsed/email/:id', component: ParsedEmail },
    { path: '/emails', component: Emails },
    { path: '/analytics', component: Analytics },
    { path: '/', redirect: '/upload' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
