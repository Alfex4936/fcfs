import Hero from '../components/Hero';
import PostList from '../components/PostList';
import { useAuth } from '../hooks/useAuth';

const HomePage = () => {
  const { isAuthenticated } = useAuth();

  return (
    <div>
      {!isAuthenticated ? (
        <Hero />
      ) : (
        <div>
          <div className="text-center mb-xl">
            <h1 className="text-3xl font-bold mb-md">
              <span className="gradient-text">Available Posts</span>
            </h1>
            <p className="text-gray-600">
              Browse and claim items shared by the community
            </p>
          </div>
          <PostList />
        </div>
      )}
    </div>
  );
};

export default HomePage;
