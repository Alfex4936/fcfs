import clsx from 'clsx';

const Badge = ({ 
  children, 
  variant = 'primary',
  className = '',
  ...props 
}) => {
  const variantClasses = {
    primary: 'badge-primary',
    success: 'badge-success',
    warning: 'badge-warning',
    danger: 'badge-danger',
    gray: 'badge-gray'
  };

  const classes = clsx(
    'badge',
    variantClasses[variant],
    className
  );

  return (
    <span className={classes} {...props}>
      {children}
    </span>
  );
};

export default Badge;
