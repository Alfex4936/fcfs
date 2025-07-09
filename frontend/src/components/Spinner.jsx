import clsx from 'clsx';

const Spinner = ({ 
  size = 'md',
  className = '',
  ...props 
}) => {
  const sizeClasses = {
    sm: 'spinner-sm',
    md: '',
    lg: 'spinner-lg'
  };

  const classes = clsx(
    'spinner',
    sizeClasses[size],
    className
  );

  return <div className={classes} {...props} />;
};

export default Spinner;
