import axios from 'axios';

const API_BASE = '/api';

// Create axios instance with default config
const apiClient = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor for logging
apiClient.interceptors.request.use(
  (config) => {
    console.log('Request:', config.method.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

// Add response interceptor for handling errors
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('Response error:', error.response?.status, error.message);
    // In a real app, you'd want proper error handling here
    if (error.response?.status === 401) {
      // Handle authentication errors
      throw new Error('Authentication required');
    }
    return Promise.reject(error);
  }
);

// Messages API - get all messages from mongsterServer
const messagesApi = {
  getAll: () => apiClient.get('/messages'),
};

// Clear API - clear stored messages via mongsterServer
const clearApi = {
  clear: () => apiClient.post('/clear'),
};

export const ApiProvider = ({ children }) => {
  return (
    <React.Fragment>
      {/* API provider would contain token management if needed */}
      {children}
    </React.Fragment>
  );
};

export { messagesApi, clearApi };
