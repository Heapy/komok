import { create } from 'zustand';

interface RouterState {
  currentPath: string;
  navigate: (path: string) => void;
}

export const useRouter = create<RouterState>((set) => ({
  currentPath: window.location.pathname,
  navigate: (path: string) => {
    window.history.pushState({}, '', path);
    set({ currentPath: path });
  },
}));

// Handle browser back/forward buttons
window.addEventListener('popstate', () => {
  useRouter.setState({ currentPath: window.location.pathname });
});