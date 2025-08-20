import * as React from 'react';
import { useAuth } from '../stores/auth';

export const HomePage: React.FC = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="container">
      <div className="hero">
        <h1 className="title">Welcome to Komok</h1>
        {user && (
          <div className="subtitle">
            Logged in as: {user.email}
          </div>
        )}
      </div>
      
      {user && (
        <div className="section">
          <button className="button light" onClick={handleLogout}>
            Logout
          </button>
        </div>
      )}
    </div>
  );
};