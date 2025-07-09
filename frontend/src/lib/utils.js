import { clsx } from 'clsx';

/**
 * Merge classes with clsx
 */
export function cn(...inputs) {
  return clsx(inputs);
}

/**
 * Format date to human readable string
 */
export function formatDate(date) {
  if (!date) return 'Invalid date';
  
  try {
    const dateObj = new Date(date);
    if (isNaN(dateObj.getTime())) {
      return 'Invalid date';
    }
    
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(dateObj);
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid date';
  }
}

/**
 * Format date to relative time (e.g., "2 hours ago")
 */
export function formatRelativeTime(date) {
  const now = new Date();
  const target = new Date(date);
  const diffInSeconds = Math.floor((now - target) / 1000);

  if (diffInSeconds < 60) {
    return 'just now';
  }

  const diffInMinutes = Math.floor(diffInSeconds / 60);
  if (diffInMinutes < 60) {
    return `${diffInMinutes} minute${diffInMinutes > 1 ? 's' : ''} ago`;
  }

  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) {
    return `${diffInHours} hour${diffInHours > 1 ? 's' : ''} ago`;
  }

  const diffInDays = Math.floor(diffInHours / 24);
  return `${diffInDays} day${diffInDays > 1 ? 's' : ''} ago`;
}

/**
 * Check if a date is in the future
 */
export function isFuture(date) {
  return new Date(date) > new Date();
}

/**
 * Check if a date is in the past
 */
export function isPast(date) {
  return new Date(date) < new Date();
}

/**
 * Get time remaining until a date
 */
export function getTimeRemaining(date) {
  const now = new Date();
  const target = new Date(date);
  const diff = target - now;

  if (diff <= 0) {
    return { expired: true };
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((diff % (1000 * 60)) / 1000);

  return { days, hours, minutes, seconds, expired: false };
}

/**
 * Debounce function
 */
export function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

/**
 * Throttle function
 */
export function throttle(func, limit) {
  let inThrottle;
  return function() {
    const args = arguments;
    const context = this;
    if (!inThrottle) {
      func.apply(context, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
}

/**
 * Copy text to clipboard
 */
export async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    try {
      document.execCommand('copy');
      return true;
    } catch {
      return false;
    } finally {
      document.body.removeChild(textArea);
    }
  }
}

/**
 * Generate avatar URL based on user email/name
 */
export function getAvatarUrl(email, size = 40) {
  // Using Gravatar as a fallback
  const hash = btoa(email).slice(0, 32);
  return `https://www.gravatar.com/avatar/${hash}?s=${size}&d=identicon`;
}

/**
 * Validate email format
 */
export function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * Generate random ID
 */
export function generateId() {
  return Math.random().toString(36).substr(2, 9);
}

/**
 * File size formatter
 */
export function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Generate placeholder image URL
 */
export function getPlaceholderImage(width = 300, height = 200, text = 'No Image') {
  // Using a placeholder service like placeholder.com or picsum.photos
  return `https://via.placeholder.com/${width}x${height}/f3f4f6/9ca3af?text=${encodeURIComponent(text)}`;
}

/**
 * Generate avatar placeholder
 */
export function getAvatarPlaceholder(name, size = 40) {
  const initials = name
    ? name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
    : '??';
  
  // Generate a consistent color based on the name
  const colors = [
    '#667eea', '#764ba2', '#f093fb', '#4facfe', '#43e97b',
    '#fa709a', '#fee140', '#a8edea', '#ff9a9e', '#c471f5'
  ];
  
  const colorIndex = name 
    ? name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % colors.length
    : 0;
  
  const bgColor = colors[colorIndex].replace('#', '');
  
  return `https://via.placeholder.com/${size}x${size}/${bgColor}/ffffff?text=${initials}`;
}

/**
 * Get optimized image URL with fallback
 */
export function getOptimizedImageUrl(originalUrl, width = 300, height = 200, fallbackText = 'Image') {
  if (!originalUrl) {
    return getPlaceholderImage(width, height, fallbackText);
  }
  
  // If it's already a placeholder or external URL, return as is
  if (originalUrl.includes('placeholder.com') || originalUrl.includes('via.placeholder') || originalUrl.startsWith('http')) {
    return originalUrl;
  }
  
  // For local API images, return the original URL
  return originalUrl;
}
