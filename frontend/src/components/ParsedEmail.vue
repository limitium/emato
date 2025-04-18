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
                
                <div class="row mb-3">
                  <div class="col-md-4">
                    <div class="mb-3">
                      <label class="form-label">Side</label>
                      <select class="form-select" v-model="trade.clientWay">
                        <option value="Buy">Buy</option>
                        <option value="Sell">Sell</option>
                      </select>
                    </div>
                  </div>
                  <div class="col-md-4">
                    <div class="mb-3">
                      <label class="form-label">Currency</label>
                      <input type="text" class="form-control" v-model="trade.currency" placeholder="USD, EUR, GBP...">
                    </div>
                  </div>
                  <div class="col-md-4">
                    <div class="mb-3">
                      <label class="form-label">ISIN Code</label>
                      <input type="text" class="form-control" v-model="trade.isinCode" placeholder="e.g. US0378331005">
                    </div>
                  </div>
                </div>
                
                <div class="row mb-3">
                  <div class="col-md-4">
                    <div class="mb-3">
                      <label class="form-label">Security Code</label>
                      <input type="text" class="form-control" v-model="trade.securityCode" placeholder="e.g. AAPL">
                    </div>
                  </div>
                  <div class="col-md-4">
                    <div class="mb-3">
                      <label class="form-label">Notional</label>
                      <input type="number" class="form-control" v-model="trade.notional" placeholder="e.g. 10000">
                    </div>
                  </div>
                  <div class="col-md-4">
                    <div class="mb-3">
                      <label class="form-label">Price</label>
                      <input type="number" class="form-control" v-model="trade.price" placeholder="e.g. 150.50">
                    </div>
                  </div>
                </div>
                
                <div class="row">
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label">Trade Date</label>
                      <input type="date" class="form-control" 
                             :value="formatDateForInput(trade.tradeDate)"
                             @input="updateTradeDate(trade, $event)">
                    </div>
                  </div>
                  <div class="col-md-6">
                    <div class="mb-3">
                      <label class="form-label">Settlement Date</label>
                      <input type="date" class="form-control" 
                             :value="formatDateForInput(trade.settlementDate)"
                             @input="updateSettlementDate(trade, $event)">
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
    formatDateForInput(dateString) {
      // Convert from format like '2024-04-10T00:00' to '2024-04-10'
      if (!dateString) return '';
      return dateString.split('T')[0];
    },
    updateTradeDate(trade, event) {
      // Update the trade date when the input changes
      const newDate = event.target.value;
      trade.tradeDate = newDate ? `${newDate}T00:00` : '';
    },
    updateSettlementDate(trade, event) {
      // Update the settlement date when the input changes
      const newDate = event.target.value;
      trade.settlementDate = newDate ? `${newDate}T00:00` : '';
    },
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
        // Prepare the update payload with only the necessary fields
        const updatePayload = {
          id: this.id,
          subject: this.emailDetails.subject,
          fromEmail: this.emailDetails.from,
          body: this.emailDetails.body,
          trades: this.trades.map(trade => ({
            id: trade.id,
            emailId: trade.emailId,
            isSuccess: trade.isSuccess,
            errorMessage: trade.errorMessage,
            clientWay: trade.clientWay,
            currency: trade.currency,
            isinCode: trade.isinCode,
            securityCode: trade.securityCode,
            notional: trade.notional || 0,
            price: trade.price || 0,
            tradeDate: trade.tradeDate || new Date().toISOString().split('T')[0] + 'T00:00',
            settlementDate: trade.settlementDate || new Date().toISOString().split('T')[0] + 'T00:00',
            // Include other required fields
            quantity: trade.quantity || 0.0,
            schemaIdentifier: trade.schemaIdentifier,
            schemaType: trade.schemaType,
            schemaVersion: trade.schemaVersion,
            solveHeader: trade.solveHeader,
            clientId: trade.clientId || "CLIENT_" + this.id,
            brokerId: trade.brokerId || "BROKER_" + this.id,
            createdAt: trade.createdAt || new Date().toISOString()
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
  color: #000000;
  margin-bottom: 0.5rem;
}

.form-control, .form-select {
  height: 45px;
  font-size: 1rem;
  color: #000000;
  border: 1px solid var(--border-color);
  border-radius: 4px;
}

.form-control:focus, .form-select:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 0.2rem rgba(0, 123, 255, 0.25);
}

.form-control::placeholder {
  color: #adb5bd;
  font-style: italic;
}

.card-body {
  padding: 1.5rem;
}

.alert-danger {
  background-color: rgba(220, 53, 69, 0.1);
  color: #dc3545;
  border-color: rgba(220, 53, 69, 0.2);
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
