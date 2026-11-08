import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    // 상수/훅/variants를 컴포넌트와 함께 export하는 shadcn 생성 파일 및 컨텍스트 파일 한정
    files: [
      'src/app/components/ui/**/*.{ts,tsx}',
      'src/components/ui/index.tsx',
      'src/contexts/AppContext.tsx',
    ],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
])
