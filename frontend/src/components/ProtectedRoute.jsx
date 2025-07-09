import { Navigate, useLocation } from 'react-router-dom';
import LoadingPage from '../components/LoadingPage';
import { useAuth } from '../hooks/useAuth';

const ProtectedRoute = ({ children, adminOnly = false }) => {
  const { isAuthenticated, loading, user } = useAuth();
  const location = useLocation();

  if (loading) {
    return <LoadingPage message="Checking authentication..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (adminOnly && !user?.includes('ADMIN')) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
