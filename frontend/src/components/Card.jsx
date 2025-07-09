import clsx from 'clsx';

const Card = ({ 
  children, 
  className = '', 
  variant = 'default',
  hover = true,
  ...props 
}) => {
  const baseClasses = 'card';
  const variantClasses = {
    default: '',
    glass: 'card-glass'
  };

  const classes = clsx(
    baseClasses,
    variantClasses[variant],
    {
      'hover:shadow-xl hover:scale-105': hover
    },
    className
  );

  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
};

const CardHeader = ({ children, className = '', ...props }) => {
  return (
    <div className={clsx('card-header', className)} {...props}>
      {children}
    </div>
  );
};

const CardBody = ({ children, className = '', ...props }) => {
  return (
    <div className={clsx('card-body', className)} {...props}>
      {children}
    </div>
  );
};

const CardFooter = ({ children, className = '', ...props }) => {
  return (
    <div className={clsx('card-footer', className)} {...props}>
      {children}
    </div>
  );
};

Card.Header = CardHeader;
Card.Body = CardBody;
Card.Footer = CardFooter;

export default Card;
