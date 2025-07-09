import { ArrowRight, Clock, Shield, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';

const Hero = () => {
  return (
    <div className="text-center py-3xl">
      <div className="mb-xl">
        <h1 className="text-5xl font-bold mb-lg">
          <span className="gradient-text">First Come, First Serve</span>
        </h1>
        <p className="text-xl text-gray-600 max-w-2xl mx-auto">
          Share resources, claim items, and connect with your community. 
          The fairest way to distribute resources - first come, first serve.
        </p>
      </div>
      
      <div className="flex flex-col sm:flex-row gap-md justify-center mb-2xl">
        <Button
          as={Link}
          to="/posts"
          variant="primary"
          size="lg"
          className="shadow-glow"
        >
          Browse Posts
          <ArrowRight size={20} />
        </Button>
        <Button
          as={Link}
          to="/create"
          variant="outline"
          size="lg"
        >
          Share Something
        </Button>
      </div>

      <div className="grid md:grid-cols-3 gap-lg max-w-4xl mx-auto">
        <Card className="text-center">
          <Card.Body>
            <div className="w-16 h-16 bg-gradient-to-br from-blue-500 to-purple-600 rounded-xl flex items-center justify-center mx-auto mb-md">
              <Users className="text-white" size={24} />
            </div>
            <h3 className="text-lg font-semibold mb-sm">Community Driven</h3>
            <p className="text-gray-600 text-sm">
              Built for communities to share resources and help each other out.
            </p>
          </Card.Body>
        </Card>

        <Card className="text-center">
          <Card.Body>
            <div className="w-16 h-16 bg-gradient-to-br from-green-500 to-teal-600 rounded-xl flex items-center justify-center mx-auto mb-md">
              <Clock className="text-white" size={24} />
            </div>
            <h3 className="text-lg font-semibold mb-sm">Fair Distribution</h3>
            <p className="text-gray-600 text-sm">
              First come, first serve ensures everyone gets an equal chance.
            </p>
          </Card.Body>
        </Card>

        <Card className="text-center">
          <Card.Body>
            <div className="w-16 h-16 bg-gradient-to-br from-orange-500 to-red-600 rounded-xl flex items-center justify-center mx-auto mb-md">
              <Shield className="text-white" size={24} />
            </div>
            <h3 className="text-lg font-semibold mb-sm">Secure & Trusted</h3>
            <p className="text-gray-600 text-sm">
              OAuth authentication and secure claim management system.
            </p>
          </Card.Body>
        </Card>
      </div>
    </div>
  );
};

export default Hero;
