<template>
  <div class="upload-container">
    <div class="upload-card">
      <h2 class="text-center">Upload Email</h2>
      <div v-if="error" class="alert alert-danger mt-3">
        <i class="fas fa-exclamation-circle me-2"></i>
        {{ error }}
      </div>
      <div class="upload-zone"
           :class="{ 'dragging': isDragging, 'disabled': isLoading }"
           @dragover.prevent="isDragging = true"
           @dragleave.prevent="isDragging = false"
           @drop.prevent="onDrop">
        <div v-if="isLoading" class="loading-overlay">
          <div class="spinner-border" role="status">
            <span class="visually-hidden">Loading...</span>
          </div>
          <p class="loading-text mt-3">Processing email...</p>
        </div>
        <template v-else>
          <i class="fas fa-cloud-upload-alt upload-icon"></i>
          <p class="upload-text">Drag and Drop email (.eml) here</p>
          <p class="upload-subtext">or</p>
          <label class="btn btn-outline-primary">
            Choose File
            <input type="file" accept=".eml" @change="onFileChange($event)" class="hidden-input" />
          </label>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data() {
    return {
      error: null,
      isDragging: false,
      isLoading: false
    }
  },
  methods: {
    async onFileChange(e) {
      const file = e.target.files[0]
      if (!file) return
      this.isLoading = true
      try {
        const text = await file.text()
        await this.uploadEmail(text)
      } catch (error) {
        this.error = "Failed to read file: " + error.message
      } finally {
        this.isLoading = false
      }
    },
    async onDrop(e) {
      this.isDragging = false
      if (e.dataTransfer.files.length > 0) {
        const file = e.dataTransfer.files[0]
        this.isLoading = true
        try {
          const text = await file.text()
          await this.uploadEmail(text)
        } catch (error) {
          this.error = "Failed to read file: " + error.message
        } finally {
          this.isLoading = false
        }
      }
    },
    async uploadEmail(body) {
      this.error = null
      try {
        const resp = await axios.post('/api/parse', { body })
        this.$router.push('/parsed/email/' + resp.data.id)
      } catch (e) {
        // Check for X-Error-Message header
        const errorHeader = e.response?.headers?.['x-error-message']
        if (errorHeader) {
          this.error = errorHeader
        } else {
          this.error = "Failed to upload: " + (e.response?.status ? 
            `Server error (${e.response.status}): ${e.response.data || 'Unknown error'}` : 
            e.message)
        }
      }
    }
  }
}
</script>

<style scoped>
.upload-container {
  min-height: calc(100vh - 56px); /* Subtract navbar height */
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: 2rem;
  background-color: var(--background-color);
}

.upload-card {
  background: var(--card-background);
  border-radius: 10px;
  padding: 2rem;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.upload-zone {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 2px dashed var(--border-color);
  background-color: var(--card-background);
  border-radius: 8px;
  padding: 3rem;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
  margin: 2rem 0;
  position: relative;
}

.upload-zone.dragging {
  border-color: var(--secondary-color);
  background-color: rgba(52, 152, 219, 0.1);
}

.upload-zone.disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.upload-icon {
  font-size: 4rem;
  color: var(--secondary-color);
  margin-bottom: 1rem;
}

.upload-text {
  font-size: 1.2rem;
  margin-bottom: 0.5rem;
}

.upload-subtext {
  color: #666;
  margin-bottom: 1rem;
}

.hidden-input {
  display: none;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 8px;
  z-index: 10;
}

.spinner-border {
  width: 3rem;
  height: 3rem;
  color: white;
  border-width: 0.25em;
}

.loading-text {
  color: white;
  font-size: 1.2rem;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
</style>