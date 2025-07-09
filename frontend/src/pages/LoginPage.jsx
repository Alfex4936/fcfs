import { Github, Globe, LogIn } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import { useToast } from '../hooks/useToast';

const LoginPage = () => {
  const [loading, setLoading] = useState(false);
  const [searchParams] = useSearchParams();
  const { addToast } = useToast();

  useEffect(() => {
    const error = searchParams.get('error');
    if (error === 'oauth_failed') {
      addToast('OAuth login failed. Please try again.', 'error');
    }
  }, [searchParams, addToast]);

  const handleOAuthLogin = (provider) => {
    setLoading(true);
    // Redirect to OAuth provider
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100 p-md">
      <div className="w-full max-w-md">
        <div className="text-center mb-xl">
          <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-md">
            <LogIn className="text-white" size={24} />
          </div>
          <h1 className="text-3xl font-bold mb-sm">
            <span className="gradient-text">Welcome Back</span>
          </h1>
          <p className="text-gray-600">Sign in to continue to FCFS</p>
        </div>

        <Card>
          <Card.Body className="space-y-md">
            <Button
              onClick={() => handleOAuthLogin('google')}
              loading={loading}
              className="w-full"
              variant="outline"
              size="lg"
            >
              <Globe size={20} />
              Continue with Google
            </Button>

            <Button
              onClick={() => handleOAuthLogin('github')}
              loading={loading}
              className="w-full"
              variant="ghost"
              size="lg"
            >
              <Github size={20} />
              Continue with GitHub
            </Button>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white text-gray-500">or</span>
              </div>
            </div>

            <div className="text-center">
              <p className="text-sm text-gray-600">
                New to FCFS?{' '}
                <Link to="/register" className="font-medium text-primary hover:text-primary-dark">
                  Create an account
                </Link>
              </p>
            </div>
          </Card.Body>
        </Card>

        <div className="mt-lg text-center">
          <p className="text-xs text-gray-500">
            By signing in, you agree to our{' '}
            <a href="#" className="text-primary hover:text-primary-dark">Terms of Service</a>
            {' '}and{' '}
            <a href="#" className="text-primary hover:text-primary-dark">Privacy Policy</a>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
