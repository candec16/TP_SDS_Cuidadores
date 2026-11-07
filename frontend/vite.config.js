import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Configuración de Vite en puerto fijo 5173 (coincide con CORS de Spring Security)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true
  }
});

