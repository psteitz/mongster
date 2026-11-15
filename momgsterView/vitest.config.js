import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'happy-dom',
    include: ['test/*.test.js', 'test/**/*.test.jsx'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: ['node_modules/**'],
    },
    setupFiles: ['./test/setup.js'],
  },
  resolve: {
    alias: {
      '@': '/src',
    },
  },
});
