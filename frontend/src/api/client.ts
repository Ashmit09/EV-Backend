import axios, { AxiosError } from 'axios';
import type { ApiError } from '../types';

export const TOKEN_KEY = 'voltx.token';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8085/api',
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = readToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let onUnauthorized: (() => void) | null = null;

/** The auth context registers a handler so an expired token logs the user out everywhere. */
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isAuthCall = error.config?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && !isAuthCall && onUnauthorized) {
      onUnauthorized();
    }
    return Promise.reject(error);
  },
);

export function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiError> | undefined;
    if (data && typeof data.message === 'string') {
      return {
        status: error.response?.status ?? 0,
        code: data.code ?? 'ERROR',
        message: data.message,
        fieldErrors: data.fieldErrors ?? {},
      };
    }
    if (!error.response) {
      return { status: 0, code: 'NETWORK', message: 'Cannot reach the VoltX server. Is the backend running?', fieldErrors: {} };
    }
  }
  return { status: 0, code: 'UNKNOWN', message: 'Something went wrong. Please try again.', fieldErrors: {} };
}

export function errorMessage(error: unknown): string {
  return toApiError(error).message;
}
