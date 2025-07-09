import clsx from 'clsx';
import { forwardRef } from 'react';

const Textarea = forwardRef(({ 
  label,
  error,
  className = '',
  ...props 
}, ref) => {
  const textareaClasses = clsx(
    'textarea',
    {
      'input-error': error
    },
    className
  );

  return (
    <div className="input-group">
      {label && (
        <label className="input-label">
          {label}
        </label>
      )}
      <textarea
        ref={ref}
        className={textareaClasses}
        {...props}
      />
      {error && (
        <div className="error-message">
          {error}
        </div>
      )}
    </div>
  );
});

Textarea.displayName = 'Textarea';

export default Textarea;
