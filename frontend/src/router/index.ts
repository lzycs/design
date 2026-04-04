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
      path: '/collaboration',
      name: 'collaboration',
      component: () => import('../views/CollaborationView.vue'),
    },
    {
      path: '/team-chat/:id',
      name: 'team-chat',
      component: () => import('../views/TeamChatView.vue'),
    },
    {
      path: '/reservation/classroom/:id',
      name: 'classroom-detail',
      component: () => import('../views/ClassroomDetailView.vue'),
    },
    {
      path: '/reservation/room-collab/:reservationId',
      name: 'room-collab',
      component: () => import('../views/RoomCollabView.vue'),
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
    {
      path: '/feedback',
      name: 'feedback',
      component: () => import('../views/FeedbackView.vue'),
    },
    {
      path: '/shared-plan',
      name: 'shared-plan',
      component: () => import('../views/SharedPlanView.vue'),
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('../views/admin/AdminLoginView.vue'),
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
    {
      path: '/admin/repairs',
      name: 'admin-repairs',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
    {
      path: '/admin/reviews',
      name: 'admin-reviews',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
    {
      path: '/admin/classrooms',
      name: 'admin-classrooms',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
    {
      path: '/admin/courses',
      name: 'admin-courses',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
    {
      path: '/admin/buildings',
      name: 'admin-buildings',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
    {
      path: '/admin/reservation-limits',
      name: 'admin-reservation-limits',
      component: () => import('../views/admin/AdminAppView.vue'),
    },
  ],
})

export default router
