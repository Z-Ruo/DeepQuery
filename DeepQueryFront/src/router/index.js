import { createRouter, createWebHistory } from 'vue-router';
import LoginForm from '@/components/LoginForm.vue';
import ChatView from '@/views/ChatView.vue';
import RagView from '@/views/RagView.vue';
import SessionsView from '@/views/SessionsView.vue';
import ProfileView from '@/views/ProfileView.vue';
import KnowledgeBaseAdminView from '@/views/KnowledgeBaseAdminView.vue'; // 新增导入
import { isLoggedIn } from '@/api/auth'; // 用于检查登录状态

const routes = [
  {
    path: '/',
    redirect: () => {
      // 如果已登录，重定向到聊天页，否则重定向到登录页
      return isLoggedIn() ? '/chat' : '/login';
    }
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginForm,
    beforeEnter: (to, from, next) => {
      // 如果已登录，尝试访问登录页时，重定向到聊天页
      if (isLoggedIn()) {
        next('/chat');
      } else {
        next();
      }
    }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatView,
    meta: { requiresAuth: true } // 此路由需要认证
  },
  {
    path: '/rag',
    name: 'RAG',
    component: RagView,
    meta: { requiresAuth: true } // 此路由需要认证
  },
  {
    path: '/sessions',
    name: 'Sessions',
    component: SessionsView,
    meta: { requiresAuth: true } // 此路由需要认证
  },
  {
    path: '/profile',
    name: 'Profile',
    component: ProfileView,
    meta: { requiresAuth: true } // 此路由需要认证
  },
  {
    path: '/admin/knowledge-base', // 新增知识库管理路由
    name: 'KnowledgeBaseAdmin',
    component: KnowledgeBaseAdminView
  }
  // 可以添加一个404页面
  // { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFoundView }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const loggedIn = isLoggedIn();

  if (requiresAuth && !loggedIn) {
    // 如果路由需要认证但用户未登录，则重定向到登录页
    next('/login');
  } else {
    next(); // 否则，正常导航
  }
});

export default router;
