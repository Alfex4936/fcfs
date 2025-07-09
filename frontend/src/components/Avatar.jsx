import clsx from 'clsx';

const Avatar = ({ 
  src, 
  alt, 
  name, 
  size = 'md',
  online = false,
  className = '',
  ...props 
}) => {
  const sizeClasses = {
    sm: 'avatar-sm',
    md: '',
    lg: 'avatar-lg',
    xl: 'avatar-xl'
  };

  const classes = clsx(
    'avatar',
    sizeClasses[size],
    {
      'avatar-online': online
    },
    className
  );

  const getInitials = (name) => {
    if (!name) return '?';
    return name
      .split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div className={classes} {...props}>
      {src ? (
        <img src={src} alt={alt || name} />
      ) : (
        getInitials(name)
      )}
    </div>
  );
};

export default Avatar;
