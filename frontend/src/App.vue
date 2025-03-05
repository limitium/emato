<template>
  <div>
    <nav class="navbar navbar-expand-lg custom-navbar">
      <div class="container">
        <router-link class="navbar-brand" to="/">
          <i class="fas fa-envelope-open-text me-2"></i>
          Email Parser
        </router-link>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse"
                data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false"
                aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarSupportedContent">
          <ul class="navbar-nav me-auto mb-2 mb-lg-0">
            <li class="nav-item">
              <router-link class="nav-link" to="/upload" active-class="active">
                <i class="fas fa-upload me-1"></i> Upload
              </router-link>
            </li>
            <li class="nav-item">
              <router-link class="nav-link" to="/emails" active-class="active">
                <i class="fas fa-inbox me-1"></i> Emails
              </router-link>
            </li>
            <li class="nav-item">
              <router-link class="nav-link" to="/analytics" active-class="active">
                <i class="fas fa-chart-line me-1"></i> Analytics
              </router-link>
            </li>
          </ul>
        </div>
      </div>
    </nav>
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style>
.custom-navbar {
  background-color: var(--card-background);
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
  border-bottom: 1px solid var(--border-color);
}

.navbar-brand {
  font-weight: 600;
  color: var(--primary-color) !important;
}

.nav-link {
  color: var(--primary-color) !important;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
}

.nav-link:hover {
  color: var(--secondary-color) !important;
}

.nav-link.active {
  color: var(--secondary-color) !important;
}

.nav-link.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--secondary-color);
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
