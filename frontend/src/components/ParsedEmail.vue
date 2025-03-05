<template>
  <div class="container-fluid">
    <!-- Main Content -->
    <div class="container py-4">
      <!-- Email Details Card -->
      <div class="card mb-4">
        <div class="card-header">
          <h2 class="mb-0">Email Details</h2>
        </div>
        <div class="card-body">
          <div class="row">
            <div class="col-md-12">
              <div class="mb-3">
                <label class="form-label text-muted">From:</label>
                <div>{{ emailDetails.from || 'N/A' }}</div>
              </div>
              <div class="mb-3">
                <label class="form-label text-muted">Subject:</label>
                <div>{{ emailDetails.subject || 'N/A' }}</div>
              </div>
              <div class="mb-3">
                <label class="form-label text-muted">Body:</label>
                <div class="email-body-container">
                  <iframe v-if="hasHtmlContent" 
                          :srcDoc="emailDetails.body"
                          class="email-body-frame">
                  </iframe>
                  <div v-else class="email-body-text">{{ emailDetails.body }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Trades Card -->
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h2 class="mb-0">Trades from Email #{{id}}</h2>
          <div>
            <button type="button" class="btn btn-outline-secondary me-2" @click="$router.push('/emails')">
              <i class="fas fa-arrow-left me-1"></i>Back
            </button>
            <button type="submit" 
                    class="btn btn-primary" 
                    @click="onSubmit"
                    :disabled="isSaving">
              <span v-if="isSaving" class="spinner-border spinner-border-sm me-1" role="status">
                <span class="visually-hidden">Saving...</span>
              </span>
              <i v-else class="fas fa-save me-1"></i>
              {{ isSaving ? 'Saving...' : 'Save' }}
            </button>
          </div>
        </div>
        <div class="card-body">
          <div v-if="error" class="alert alert-danger">{{ error }}</div>
          
          <!-- Trade List -->
          <div v-for="(trade, index) in trades" :key="trade.id" class="trade-card mb-4">
            <div class="card">
              <div class="card-header d-flex justify-content-between align-items-center" 
                   :class="{'bg-success text-white': trade.isSuccess, 'bg-danger text-white': !trade.isSuccess}">
                <h3 class="mb-0">Trade #{{ trade.id }}</h3>
                <div class="d-flex align-items-center gap-2">
                  <span v-if="!trade.isSuccess" class="badge bg-warning text-dark">
                    <i class="fas fa-exclamation-triangle me-1"></i>Failed
                  </span>
                </div>
              </div>
              <div class="card-body">
                <div v-if="!trade.isSuccess" class="alert alert-danger mb-3">
                  {{ trade.errorMessage }}
                </div>
                <div class="row">
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label">Client Way</label>
                      <select class="form-select" v-model="trade.clientWay">
                        <option value="Buy">Buy</option>
                        <option value="Sell">Sell</option>
                      </select>
                    </div>
                    <div class="mb-3">
                      <label class="form-label">Currency</label>
                      <select class="form-select" v-model="trade.currency">
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                      </select>
                    </div>
                    <div class="mb-3">
                      <label class="form-label">ISIN Code</label>
                      <input type="text" class="form-control" v-model="trade.isinCode">
                    </div>
                    <div class="mb-3">
                      <label class="form-label">Quantity</label>
                      <input type="number" class="form-control" v-model.number="trade.quantity">
                    </div>
                  </div>
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label">Price</label>
                      <input type="number" step="0.01" class="form-control" v-model.number="trade.price">
                    </div>
                    <div class="mb-3">
                      <label class="form-label">Trade Date</label>
                      <input type="date" class="form-control" v-model="trade.tradeDate">
                    </div>
                    <div class="mb-3">
                      <label class="form-label">Settlement Date</label>
                      <input type="date" class="form-control" v-model="trade.settlementDate">
                    </div>
                    <div class="mb-3">
                      <label class="form-label">Broker Alias</label>
                      <input type="text" class="form-control" v-model="trade.brokerAlias">
                    </div>
                  </div>
                </div>
              </div>
            </div>
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
      id: null,
      emailDetails: {
        from: '',
        subject: '',
        body: ''
      },
      trades: [],
      error: null,
      isSaving: false
    }
  },
  computed: {
    hasHtmlContent() {
      const body = this.emailDetails.body || '';
      return body.toLowerCase().includes('<html');
    }
  },
  methods: {
    async loadData() {
      try {
        const resp = await axios.get('/api/email/' + this.id)
        
        // Load email details
        this.emailDetails = {
          from: resp.data.fromEmail || '',
          subject: resp.data.subject || '',
          body: resp.data.body || ''
        }
        
        // Load trades
        this.trades = resp.data.trades || []
        
      } catch (e) {
        this.error = "Failed to load data: " + e.response?.data || e.message
      }
    },
    async onSubmit() {
      if (this.isSaving) return
      
      this.isSaving = true
      try {
        // Prepare the update payload
        const updatePayload = {
          id: this.id,
          subject: this.emailDetails.subject,
          fromEmail: this.emailDetails.from,
          body: this.emailDetails.body,
          trades: this.trades.map(trade => ({
            ...trade,
            // Ensure numeric fields are properly typed
            quantity: Number(trade.quantity),
            price: Number(trade.price)
          })),
          sent: false,
          createdAt: new Date().toISOString(),
          modifiedAt: new Date().toISOString()
        }

        const resp = await axios.put('/api/email/' + this.id, updatePayload)
        this.error = null
        
        // Show success message
        this.$emit('show-toast', {
          type: 'success',
          message: 'Changes saved successfully'
        })
        
        // Navigate to emails list
        this.$router.push('/emails')
      } catch (e) {
        this.error = "Failed to save: " + e.response?.data || e.message
      } finally {
        this.isSaving = false
      }
    }
  },
  created() {
    this.id = this.$route.params.id
    this.loadData()
  }
}
</script>

<style scoped>
.card {
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.card-header {
  background-color: var(--card-background);
  border-bottom: 1px solid var(--border-color);
}

.form-label {
  font-weight: 500;
  color: var(--text-muted);
}

.btn-secondary {
  background-color: var(--input-background);
  border-color: var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--border-color);
  border-color: var(--border-color);
}

.email-body-container {
  border: 1px solid var(--border-color);
  border-radius: 0.25rem;
  background-color: white;
  overflow: hidden;
  margin-bottom: 1rem;
}

.email-body-frame {
  width: 100%;
  height: 400px;
  border: none;
}

.email-body-text {
  white-space: pre-wrap;
  font-family: var(--font-family-sans-serif);
  padding: 1rem;
  max-height: 400px;
  overflow-y: auto;
}

.trade-card .card {
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.trade-card .card-header {
  padding: 1rem;
}

.trade-card .card-header h3 {
  font-size: 1.1rem;
  font-weight: 500;
}

.trade-card .form-label {
  font-weight: 500;
  color: var(--text-muted);
}

.trade-card .form-control:disabled {
  background-color: #f8f9fa;
  opacity: 0.8;
}

.badge {
  font-weight: 500;
  padding: 0.5em 0.75em;
}

.btn-light {
  background-color: rgba(255, 255, 255, 0.9);
  border-color: rgba(255, 255, 255, 0.5);
}

.btn-light:hover {
  background-color: #fff;
  border-color: #fff;
}

.gap-2 {
  gap: 0.5rem;
}

.trade-card .form-control:not(:disabled) {
  background-color: #fff;
  border-color: var(--border-color);
}

.trade-card .form-control:not(:disabled):focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.form-select, .form-control {
  background-color: white;
  border: 1px solid var(--border-color);
  color: #000000;
}

.form-select:focus, .form-control:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
  color: #000000;
}

.btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.spinner-border-sm {
  width: 1rem;
  height: 1rem;
  border-width: 0.2em;
}

/* Add styles for form inputs */
input::placeholder {
  color: #6c757d;
}

/* Ensure consistent text color in disabled state */
.form-control:disabled,
.form-select:disabled {
  background-color: #e9ecef;
  color: #495057;
}

/* Improve contrast for select options */
.form-select option {
  color: #000000;
}

/* Ensure consistent styling across browsers */
input[type="date"] {
  color: #000000;
}

input[type="number"] {
  color: #000000;
}
</style>
