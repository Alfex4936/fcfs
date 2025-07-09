import clsx from 'clsx';
import { X } from 'lucide-react';
import { useEffect } from 'react';
import { useToast } from '../hooks/useToast';

const Toast = ({ toast }) => {
  const { removeToast } = useToast();

  useEffect(() => {
    if (toast.duration > 0) {
      const timer = setTimeout(() => {
        removeToast(toast.id);
      }, toast.duration);

      return () => clearTimeout(timer);
    }
  }, [toast.id, toast.duration, removeToast]);

  const typeClasses = {
    success: 'toast-success',
    error: 'toast-error',
    warning: 'toast-warning',
    info: 'toast-info'
  };

  return (
    <div className={clsx('toast', typeClasses[toast.type])}>
      <div className="flex items-start gap-md">
        <div className="flex-1">
          {toast.message}
        </div>
        <button
          onClick={() => removeToast(toast.id)}
          className="text-gray-400 hover:text-gray-600 transition-colors"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );
};

const ToastContainer = () => {
  const { toasts } = useToast();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-lg right-lg z-50 space-y-2">
      {toasts.map(toast => (
        <Toast key={toast.id} toast={toast} />
      ))}
    </div>
  );
};

export default ToastContainer;
