import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    root: 'src',
    plugins: [vue()],
    build: {
        outDir: '../../src/main/resources/static',
        emptyOutDir: true
    },
    server: {
        port: 3000,
        proxy: {
            '/api': 'http://localhost:8080'
        }
    }
})
