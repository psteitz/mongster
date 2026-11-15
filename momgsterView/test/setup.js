/**
 * Vitest setup file for monsgterView tests
 */

import '@testing-library/jest-dom';

// Configure console methods for testing
const originalConsoleError = console.error;
const originalConsoleWarn = console.warn;

console.error = (...args) => {
  // Only log errors in test mode
  if (process.env.NODE_ENV !== 'test') {
    return originalConsoleError(...args);
  }
};

console.warn = (...args) => {
  // Warn but don't fail tests for non-critical warnings
  if (process.env.NODE_ENV !== 'test' || !args[0].includes('DEPRECATED')) {
    return originalConsoleWarn(...args);
  }
};

// Global mocks
global.fetch = jest.fn(() =>
  Promise.resolve({
    ok: true,
    json: () => Promise.resolve({}),
  })
);

console.log('Vitest setup complete');
