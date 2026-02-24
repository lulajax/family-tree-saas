import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/groups'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { guest: true }
    },
    {
      path: '/groups',
      name: 'GroupList',
      component: () => import('@/views/GroupList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/groups/:id',
      name: 'GroupDetail',
      component: () => import('@/views/GroupDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/groups/:id/tree',
      name: 'TreeView',
      component: () => import('@/views/TreeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/groups/:id/members',
      name: 'MemberList',
      component: () => import('@/views/MemberList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/workspace/:groupId',
      name: 'Workspace',
      component: () => import('@/views/Workspace.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/person/:id',
      name: 'PersonDetail',
      component: () => import('@/views/PersonDetail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/person/:id/edit',
      name: 'PersonEdit',
      component: () => import('@/views/PersonEdit.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/merge-requests',
      name: 'MergeRequestList',
      component: () => import('@/views/MergeRequestList.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.guest && userStore.isLoggedIn) {
    next('/groups')
  } else {
    next()
  }
})

export default router
