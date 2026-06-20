import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    exclude: ['node_modules/**', 'dist/**', 'srt/**', 'tests/e2e/**'],
  },
})
