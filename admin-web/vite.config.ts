import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 서버에서 Spring Boot 백엔드로 API 요청 전달
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': {
          target: env.BACKEND_URL || 'http://172.18.32.74:8081',
          changeOrigin: true,
        },
      },
    },
  }
})