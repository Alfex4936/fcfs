# FCFS Frontend

A beautiful, modern React frontend for the First Come First Serve sharing platform.

## ✨ Features

- **Premium UI/UX** - Beautiful, responsive design with modern animations
- **OAuth Authentication** - Support for Google, Naver, and Kakao login
- **Real-time Updates** - Live claiming status and countdown timers
- **Admin Dashboard** - Complete post and user management
- **Responsive Design** - Works perfectly on all devices
- **Dark Mode Ready** - Prepared for future dark mode support

## 🚀 Tech Stack

- **React 19** - Latest React with modern features
- **Vite** - Fast build tool and dev server
- **Tailwind CSS** - Utility-first CSS framework
- **React Router** - Client-side routing
- **TanStack Query** - Server state management
- **Axios** - HTTP client
- **Lucide React** - Beautiful icons

## 🛠️ Development

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Environment Setup

The frontend expects the backend API to be running on `http://localhost:8080`. 

For OAuth to work properly, make sure your backend is configured with the correct redirect URIs pointing to `http://localhost:3000/oauth2/redirect`.

## 📱 Pages & Features

### Authentication
- **Login Page** - OAuth login with Google, Naver, Kakao
- **Protected Routes** - Automatic redirect to login for unauthorized users

### Main Features
- **Home Page** - Browse and search posts with real-time status
- **Create Post** - Rich form with image upload and tagging
- **Post Details** - Full post view with claiming functionality

### Admin Features
- **Admin Dashboard** - Complete overview with statistics
- **User Management** - Remove claims and manage users
- **Post Moderation** - Delete posts and monitor activity

## 🎨 Design System

### Colors
- **Primary**: Blue (#3B82F6) - Main brand color
- **Secondary**: Slate - Supporting colors
- **Success**: Green - Positive actions
- **Warning**: Yellow - Attention states
- **Error**: Red - Error states

### Components
- **Button** - Primary, secondary, ghost variants
- **Input/Textarea** - Consistent form elements
- **Card** - Content containers with hover effects
- **Badge** - Status indicators
- **Avatar** - User profile images
- **Loading Spinner** - Loading states

### Typography
- **Font**: Inter - Modern, readable sans-serif
- **Hierarchy**: Clear heading scales
- **Line Height**: Optimized for readability

## 📋 API Integration

The frontend integrates with the Spring Boot backend through:

- **Authentication API** (`/api/user/*`)
- **Posts API** (`/api/posts/*`) 
- **Claims API** (`/api/claims/*`)
- **Admin API** (`/api/admin/*`)

All API calls include automatic:
- JWT token attachment
- Error handling
- Loading states
- Retry logic

## 🔐 Security

- **JWT Storage** - Secure token storage in localStorage
- **Route Protection** - Automatic authentication checks
- **CORS Handling** - Proper cross-origin configuration
- **Input Validation** - Client-side form validation

## 📈 Performance

- **Code Splitting** - Optimized bundle chunks
- **Lazy Loading** - Components loaded on demand
- **Image Optimization** - Responsive images
- **Caching** - Smart query caching with TanStack Query

## 🚀 Deployment

### Build

```bash
npm run build
```

The build outputs to `dist/` directory.

### Docker

```dockerfile
FROM node:18-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## 🎯 Future Enhancements

- [ ] Dark mode support
- [ ] Real-time notifications
- [ ] Progressive Web App (PWA)
- [ ] Offline support
- [ ] Advanced search filters
- [ ] Social sharing
- [ ] Multi-language support

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is part of the FCFS platform. See the main project LICENSE for details.+ Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
