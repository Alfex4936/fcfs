import { Home, Search } from 'lucide-react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';

const NotFoundPage = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="text-center">
        <div className="mb-xl">
          <div className="text-9xl font-bold text-gray-300 mb-md">404</div>
          <h1 className="text-3xl font-bold text-gray-800 mb-sm">Page Not Found</h1>
          <p className="text-gray-600 max-w-md">
            Sorry, we couldn't find the page you're looking for. 
            The page might have been removed or you entered an incorrect URL.
          </p>
        </div>
        
        <div className="flex flex-col sm:flex-row gap-md justify-center">
          <Button
            as={Link}
            to="/"
            variant="primary"
          >
            <Home size={16} />
            Go Home
          </Button>
          <Button
            as={Link}
            to="/posts"
            variant="outline"
          >
            <Search size={16} />
            Browse Posts
          </Button>
        </div>
      </div>
    </div>
  );
};

export default NotFoundPage;
