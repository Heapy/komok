import { create } from 'zustand';

interface User {
  email: string;
}

interface AuthState {
  user: User | null;
  isLoading: boolean;
  checkAuth: () => Promise<void>;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

export const useAuth = create<AuthState>((set, get) => ({
  user: null,
  isLoading: false,

  checkAuth: async () => {
    set({ isLoading: true });
    try {
      const response = await fetch('/me', {
        credentials: 'include',
      });
      
      if (response.ok) {
        const user = await response.json();
        set({ user, isLoading: false });
      } else {
        console.log('Auth check failed:', response.status);
        set({ user: null, isLoading: false });
      }
    } catch (error) {
      console.error('Auth check error:', error);
      set({ user: null, isLoading: false });
    }
  },

  login: async (email: string, password: string) => {
    set({ isLoading: true });
    try {
      const response = await fetch('/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ email, password }),
      });

      if (response.ok) {
        await get().checkAuth();
      } else {
        throw new Error('Login failed');
      }
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  logout: () => {
    set({ user: null });
    // Could add logout API call here if needed
  },
}));