import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/classrooms/:buildingId',
      name: 'classrooms',
      component: () => import('../views/ClassroomsView.vue'),
    },
    {
      path: '/reservation',
      name: 'reservation',
      component: () => import('../views/ReservationView.vue'),
    },
    {
      path: '/reservation/classroom/:id',
      name: 'classroom-detail',
      component: () => import('../views/ClassroomDetailView.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/ProfileView.vue'),
    },
    {
      path: '/profile/info',
      name: 'profile-info',
      component: () => import('../views/ProfileInfoView.vue'),
    },
    {
      path: '/profile/reservations',
      name: 'profile-reservations',
      component: () => import('../views/ProfileReservationView.vue'),
    },
    {
      path: '/profile/repairs',
      name: 'profile-repairs',
      component: () => import('../views/ProfileRepairView.vue'),
    },
    {
      path: '/profile/reviews',
      name: 'profile-reviews',
      component: () => import('../views/ProfileReviewView.vue'),
    },
    {
      path: '/profile/teams',
      name: 'profile-teams',
      component: () => import('../views/ProfileTeamView.vue'),
    },
    {
      path: '/profile/plans',
      name: 'profile-plans',
      component: () => import('../views/ProfilePlanView.vue'),
    },
  ],
})

export default router
