import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// import store from './store' // 如果使用 Vuex
// import './assets/tailwind.css' // 如果使用 Tailwind CSS
import './assets/global.css' // 一个用于全局样式的文件示例

const app = createApp(App)

app.use(router)
app.use(ElementPlus)
// app.use(store) // 如果使用 Vuex

app.mount('#app')
