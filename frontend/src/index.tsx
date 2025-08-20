import * as React from "react";
import {createRoot} from "react-dom/client";
import {HomePage} from "./components/HomePage";
import {useRouter} from "./stores/router";
import {useAuth} from "./stores/auth";
import {useEffect} from "react";
import {LoginPage} from "./components/LoginPage";

const App: React.FC = () => {
  const {currentPath} = useRouter();
  const {user, isLoading, checkAuth} = useAuth();

  useEffect(() => {
    console.log('App mounted, checking auth...');
    checkAuth();
  }, [checkAuth]);

  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
      </div>
    );
  }

  // Show login page if user is not authenticated or on /login route
  if (!user || currentPath === '/login') {
    return <LoginPage/>;
  }

  // Show appropriate component based on route
  switch (currentPath) {
    case '/':
      return <HomePage/>;
    default:
      return <HomePage/>;
  }
};

const root = document.getElementById("root") as HTMLElement;

createRoot(root)
  .render(
    <React.StrictMode>
      <App/>
    </React.StrictMode>
  );


