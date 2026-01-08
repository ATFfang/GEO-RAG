import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 引入你之前写的 router/index.ts
import './assets/main.css'   // 引入 Tailwind v4 的 CSS

const app = createApp(App)
app.use(router) // 必须使用 .use(router)
app.mount('#app')