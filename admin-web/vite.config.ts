import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// 개발 서버에서 Spring Boot 백엔드로 API 요청 전달
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = {
    target: env.BACKEND_URL || 'http://16.179.249.107:8081',
    changeOrigin: true,
  }

  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': proxyTarget,
        // 감지 이미지는 백엔드가 /images/** 로 제공한다
        '/images': proxyTarget,
      },
    },
  }
})