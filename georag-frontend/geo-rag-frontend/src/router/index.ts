import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import Home from '../views/Home.vue';
import Profile from '../views/Profile.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/home',
    name: 'Home',
    component: Home,
    // meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    // meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// // 全局前置守卫
// router.beforeEach((to, from, next) => {
//   const token = localStorage.getItem('token');
  
//   if (to.meta.requiresAuth && !token) {
//     // 强制跳转登录
//     next('/login');
//   } else if (to.path === '/login' && token) {
//     // 已登录则跳过登录页
//     next('/');
//   } else {
//     // 正常放行
//     next();
//   }
  
//   // 测试阶段如果想关闭守卫，注释掉上面代码，直接使用: next();
// });

export default router;