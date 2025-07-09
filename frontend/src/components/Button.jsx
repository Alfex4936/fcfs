import clsx from 'clsx';
import { forwardRef } from 'react';

const Button = forwardRef(({ 
  children, 
  variant = 'primary', 
  size = 'md', 
  loading = false, 
  disabled = false,
  className = '',
  type = 'button',
  onClick,
  as: Component = 'button',
  ...props 
}, ref) => {
  const baseClasses = 'btn';
  const variantClasses = {
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    success: 'btn-success',
    warning: 'btn-warning',
    danger: 'btn-danger',
    outline: 'btn-outline',
    ghost: 'btn-ghost'
  };
  const sizeClasses = {
    sm: 'btn-sm',
    md: '',
    lg: 'btn-lg',
    xl: 'btn-xl'
  };

  const classes = clsx(
    baseClasses,
    variantClasses[variant],
    sizeClasses[size],
    className
  );

  const buttonProps = {
    ref,
    className: classes,
    onClick,
    ...props
  };

  if (Component === 'button') {
    buttonProps.type = type;
    buttonProps.disabled = disabled || loading;
  }

  return (
    <Component {...buttonProps}>
      {loading && (
        <div className="spinner spinner-sm" />
      )}
      {children}
    </Component>
  );
});

Button.displayName = 'Button';

export default Button;
