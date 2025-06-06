import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 设置 API 基础 URL 如果主机名为 10.10.52.215 则使用测试环境，否则使用生产环境
const apiBaseUrl = window.location.hostname === '10.10.52.215' ? 'http://10.10.52.215:8080/v1' : 'http://111.229.211.148:8080/v1';
axios.defaults.baseURL = apiBaseUrl;

axios.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)
axios.interceptors.response.use(
    response => response,
    error => {
        if (error.response?.status === 403) {  // 使用可选链操作符
            router.push('/').then(r => "null");
            localStorage.removeItem('token');

        }
        return Promise.reject(error);
    }
);
// 将 Axios 添加到 Vue 原型中，以便在组件中使用
const app = createApp(App)
app.config.globalProperties.$axios = axios

app.use(router)
app.use(ElementPlus)
app.mount('#app')
