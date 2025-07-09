import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import LoadingPage from '../components/LoadingPage';
import { useAuth } from '../hooks/useAuth';
import { authAPI } from '../lib/api';

const OAuthCallbackPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();

  useEffect(() => {
    const handleOAuthCallback = async () => {
      try {
        // Get the token from URL parameters
        const token = searchParams.get('token');
        
        if (!token) {
          console.error('No token found in OAuth callback');
          navigate('/login?error=oauth_failed');
          return;
        }

        // Store the token temporarily to make the API call
        localStorage.setItem('token', token);
        
        // Fetch user data using the token
        const response = await authAPI.me();
        const user = response.data;
        
        // Use the login function to properly set up auth state
        login(token, user);
        
        // Redirect to home page
        navigate('/', { replace: true });
        
      } catch (error) {
        console.error('OAuth callback error:', error);
        
        // Clean up on error
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        
        // Redirect to login with error
        navigate('/login?error=oauth_failed');
      }
    };

    handleOAuthCallback();
  }, [searchParams, navigate, login]);

  return <LoadingPage message="Completing OAuth login..." />;
};

export default OAuthCallbackPage;
