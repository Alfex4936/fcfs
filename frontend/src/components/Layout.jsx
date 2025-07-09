import Navbar from './Navbar';
import ToastContainer from './Toast';

const Layout = ({ children }) => {
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <Navbar />
      <main className="container py-xl">
        {children}
      </main>
      <ToastContainer />
    </div>
  );
};

export default Layout;
