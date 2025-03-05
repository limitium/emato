<template>
  <div class="container-fluid">
    <!-- Main Content -->
    <div class="container py-4">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h2 class="mb-0">Parsed Emails</h2>
          <div class="d-flex gap-2">
            <div class="input-group">
              <span class="input-group-text">
                <i class="fas fa-search"></i>
              </span>
              <input type="text" 
                     class="form-control" 
                     v-model="searchQuery" 
                     placeholder="Search emails...">
            </div>
            <select class="form-select" v-model="statusFilter">
              <option value="all">All Status</option>
              <option value="success">Success</option>
              <option value="failed">Failed</option>
              <option value="mixed">Mixed</option>
            </select>
          </div>
        </div>
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover mb-0">
              <thead>
                <tr>
                  <th>Email ID</th>
                  <th>Subject</th>
                  <th>From</th>
                  <th>Trades</th>
                  <th>Status</th>
                  <th>Created At</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="email in filteredEmails" :key="email.id">
                  <td>{{ email.id }}</td>
                  <td>{{ email.subject }}</td>
                  <td>{{ email.fromEmail }}</td>
                  <td>
                    <div class="d-flex align-items-center gap-2">
                      <span class="badge bg-primary">{{ email.trades.length }}</span>
                      <div class="trade-status-pills">
                        <span class="badge bg-success" v-if="getSuccessfulTrades(email).length">
                          {{ getSuccessfulTrades(email).length }}
                        </span>
                        <span class="badge bg-danger" v-if="getFailedTrades(email).length">
                          {{ getFailedTrades(email).length }}
                        </span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <span :class="getStatusBadgeClass(email)">
                      {{ getEmailStatus(email) }}
                    </span>
                  </td>
                  <td>{{ formatDate(email.createdAt) }}</td>
                  <td>
                    <div class="btn-group">
                      <router-link :to="'/parsed/email/' + email.id" 
                                 class="btn btn-sm btn-outline-primary">
                        <i class="fas fa-edit me-1"></i>Edit
                      </router-link>
                      <button class="btn btn-sm btn-outline-danger" 
                              @click="deleteEmail(email.id)">
                        <i class="fas fa-trash me-1"></i>Delete
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      emails: [],
      searchQuery: '',
      statusFilter: 'all',
      error: null
    }
  },
  computed: {
    filteredEmails() {
      return this.emails
        .filter(email => {
          // Search filter
          const searchLower = this.searchQuery.toLowerCase()
          const matchesSearch = !this.searchQuery || 
            email.subject.toLowerCase().includes(searchLower) ||
            email.fromEmail.toLowerCase().includes(searchLower)

          // Status filter
          const emailStatus = this.getEmailStatus(email).toLowerCase()
          const matchesStatus = this.statusFilter === 'all' || 
            emailStatus === this.statusFilter

          return matchesSearch && matchesStatus
        })
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    }
  },
  methods: {
    async loadEmails() {
      try {
        const response = await axios.get('/api/emails')
        this.emails = response.data
      } catch (e) {
        this.error = "Failed to load emails: " + e.message
      }
    },
    async deleteEmail(id) {
      if (confirm('Are you sure you want to delete this email?')) {
        try {
          await axios.delete(`/api/email/${id}`)
          this.emails = this.emails.filter(email => email.id !== id)
        } catch (e) {
          this.error = "Failed to delete email: " + e.message
        }
      }
    },
    formatDate(dateString) {
      return new Date(dateString).toLocaleString()
    },
    getSuccessfulTrades(email) {
      return email.trades.filter(trade => trade.isSuccess)
    },
    getFailedTrades(email) {
      return email.trades.filter(trade => !trade.isSuccess)
    },
    getEmailStatus(email) {
      const successful = this.getSuccessfulTrades(email).length
      const failed = this.getFailedTrades(email).length
      
      if (successful > 0 && failed > 0) return 'Mixed'
      if (successful > 0) return 'Success'
      if (failed > 0) return 'Failed'
      return 'No Trades'
    },
    getStatusBadgeClass(email) {
      const status = this.getEmailStatus(email)
      return {
        'badge': true,
        'bg-success': status === 'Success',
        'bg-danger': status === 'Failed',
        'bg-warning text-dark': status === 'Mixed',
        'bg-secondary': status === 'No Trades'
      }
    }
  },
  created() {
    this.loadEmails()
  }
}
</script>

<style scoped>
.trade-status-pills {
  display: flex;
  gap: 0.25rem;
}

.badge {
  font-weight: 500;
  padding: 0.5em 0.75em;
}

.table th {
  background-color: #f8f9fa;
  border-bottom: 2px solid #dee2e6;
}

.table td {
  vertical-align: middle;
}

.btn-group .btn {
  padding: 0.25rem 0.5rem;
}

.form-select {
  min-width: 150px;
}
</style>
