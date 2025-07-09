import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Navigate, Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthProvider';
import { ToastProvider } from './contexts/ToastProvider';
import { useAuth } from './hooks/useAuth';

import Layout from './components/Layout';
import LoadingPage from './components/LoadingPage';
import ProtectedRoute from './components/ProtectedRoute';

import AdminPage from './pages/AdminPage';
import CreatePostPage from './pages/CreatePostPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import OAuthCallbackPage from './pages/OAuthCallbackPage';

import './App.css';

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

const AppContent = () => {
  const { loading } = useAuth();

  if (loading) {
    return <LoadingPage message="Loading application..." />;
  }

  return (
    <Router>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth2/redirect" element={<OAuthCallbackPage />} />
        
        {/* Protected routes with layout */}
        <Route path="/" element={
          <Layout>
            <HomePage />
          </Layout>
        } />
        
        <Route path="/posts" element={
          <Layout>
            <HomePage />
          </Layout>
        } />
        
        <Route path="/create" element={
          <ProtectedRoute>
            <Layout>
              <CreatePostPage />
            </Layout>
          </ProtectedRoute>
        } />
        
        <Route path="/admin" element={
          <ProtectedRoute adminOnly>
            <Layout>
              <AdminPage />
            </Layout>
          </ProtectedRoute>
        } />
        
        {/* Redirect old paths */}
        <Route path="/register" element={<Navigate to="/login" replace />} />
        <Route path="/profile" element={<Navigate to="/" replace />} />
        
        {/* 404 page */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Router>
  );
};

const App = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <AuthProvider>
          <AppContent />
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  );
};

export default App;
