export default {
  env: ['browser', 'node', 'es2022'],
  extends: [
    'eslint:recommended',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    'prefer-const': 'warn',
    'no-console': 'off', // Allow console for debugging
    'semi': ['error', 'always'],
    'curly': ['error', 'all'],
  },
  ignorePatterns: ['node_modules/', 'dist/'],
};
