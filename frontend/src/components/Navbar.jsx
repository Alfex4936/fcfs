import { Home, LogOut, Plus, ShieldCheck } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import Avatar from './Avatar';
import Button from './Button';

const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const location = useLocation();

  const isAdmin = user?.includes('ADMIN'); // Adjust based on your user role structure

  const navigation = [
    { name: 'Home', href: '/', icon: Home },
    { name: 'Create Post', href: '/create', icon: Plus },
    ...(isAdmin ? [{ name: 'Admin', href: '/admin', icon: ShieldCheck }] : [])
  ];

  const isActive = (path) => location.pathname === path;

  return (
    <nav className="navbar">
      <div className="container">
        <div className="navbar-container">
          {/* Brand */}
          <Link to="/" className="navbar-brand">
            <div className="navbar-brand-logo">
              <Home size={18} />
            </div>
            <span className="gradient-text">FCFS</span>
          </Link>

          {/* Navigation Links */}
          {isAuthenticated && (
            <div className="navbar-menu md:flex hidden">
              {navigation.map((item) => {
                const Icon = item.icon;
                return (
                  <Link
                    key={item.name}
                    to={item.href}
                    className={`navbar-link ${isActive(item.href) ? 'active' : ''}`}
                  >
                    <Icon size={16} />
                    {item.name}
                  </Link>
                );
              })}
            </div>
          )}

          {/* User Menu */}
          <div className="flex items-center gap-md">
            {isAuthenticated ? (
              <div className="flex items-center gap-md">
                <Link to="/profile" className="flex items-center gap-sm">
                  <Avatar 
                    name={user} 
                    size="sm"
                  />
                  <span className="text-sm font-medium text-gray-700 hidden md:block">
                    {user}
                  </span>
                </Link>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={logout}
                  className="text-gray-600"
                >
                  <LogOut size={16} />
                  <span className="hidden md:inline">Logout</span>
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-sm">
                <Button
                  as={Link}
                  to="/login"
                  variant="ghost"
                  size="sm"
                >
                  Login
                </Button>
                <Button
                  as={Link}
                  to="/register"
                  variant="primary"
                  size="sm"
                >
                  Sign Up
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
