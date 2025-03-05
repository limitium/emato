<template>
  <div class="analytics-container">
    <h2>Analytics</h2>

    <!-- Error Alert -->
    <div v-if="error" class="alert alert-danger alert-dismissible fade show mt-3" role="alert">
      <i class="fas fa-exclamation-circle me-2"></i>
      {{ error }}
      <button type="button" class="btn-close" @click="error = null" aria-label="Close"></button>
    </div>
    
    <!-- Loading State -->
    <div v-if="loading" class="text-center my-4">
      <i class="fas fa-spinner fa-spin me-2"></i> Loading...
    </div>

    <div v-if="!loading && !error" id="funnel" class="funnel-container"></div>
  </div>
</template>

<script>
import { nextTick } from 'vue'
import FunnelGraph from 'funnel-graph-js/dist/js/funnel-graph.js'
import 'funnel-graph-js/dist/css/main.css'
import 'funnel-graph-js/dist/css/theme.css'
import axios from 'axios'

export default {
  data() {
    return {
      data: null,
      error: null,
      loading: false
    }
  },
  async mounted() {
    await this.fetchAnalytics()
  },
  methods: {
    async fetchAnalytics() {
      this.error = null
      this.loading = true

      try {
        const resp = await axios.get('/api/analytics')
        this.data = resp.data
      } catch (error) {
        this.error = error.response?.data?.message ||
                    error.response?.data ||
                    error.message ||
                    'Failed to fetch analytics data. Please try again later.'

        if (!error.response) {
          this.error = 'Unable to connect to the server. Please check your internet connection.'
        } else if (error.response.status === 404) {
          this.error = 'The analytics data could not be found.'
        } else if (error.response.status >= 500) {
          this.error = 'Server error occurred. Please try again later.'
        }
      } finally {
        this.loading = false
        if (!this.error && this.data) {
          await nextTick()
          this.renderGraph()
        }
      }
    },
    async renderGraph() {
      if (!this.data) return

      await nextTick()
      const graph = new FunnelGraph({
        container: '#funnel',
        gradientDirection: 'horizontal',
        height: 400,
        width: 800,
        data: {
          labels: ['Total Emails', 'Total Extracted', 'Total Sent', 'Total Untouched'],
          values: [
            this.data.totalEmails,
            this.data.totalExtracted,
            this.data.totalSent,
            this.data.totalUntouched
          ],
          colors: [
            '#2ecc71',
            '#3498db',
            '#9b59b6',
            '#f1c40f'
          ]
        },
        displayPercent: true,
        direction: 'horizontal'
      })
      graph.draw()
    }
  }
}
</script>

<style scoped>
.analytics-container {
  padding: 2rem;
  height: 100%;
  min-height: calc(100vh - 56px); /* Subtract navbar height */
  display: flex;
  flex-direction: column;
}

.funnel-container {
  flex: 1;
  min-height: 400px; /* Minimum height for the funnel */
  width: 100%;
  margin: 2rem auto;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* Funnel Graph Custom Styles */
:deep(.svg-funnel-js) {
  width: 100%;
  max-width: 800px;
}

:deep(.svg-funnel-js .funnel) {
  fill-opacity: 0.8;
}

:deep(.svg-funnel-js .label) {
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
}

:deep(.svg-funnel-js .percentage) {
  font-weight: 600;
}

.alert {
  animation: fadeIn 0.3s ease-in;
}

.alert-dismissible {
  padding-right: 3rem;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
