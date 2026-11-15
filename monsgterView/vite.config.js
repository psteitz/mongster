import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    host: true, // Allow external access for development
    proxy: {
      '/api/messages': {
        target: 'http://localhost:3001',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api\/messages/, ''),
      },
      '/api/clear': {
        target: 'http://localhost:3001',
        changeOrigin: true,
        rewrite: path => path.replace(/^\/api\/clear$/, ''),
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    minify: true, // Optimize for production
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'axios'],
        },
      },
    },
  },
});
