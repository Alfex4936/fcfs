import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { useToast } from '../hooks/useToast';
import { claimsAPI, postsAPI } from '../lib/api';
import { formatDate } from '../lib/utils';
import Badge from './Badge';
import Button from './Button';
import Card from './Card';
import Spinner from './Spinner';

const PostList = () => {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState(null);
  const [claimingPosts, setClaimingPosts] = useState(new Set());
  
  const { user, isAuthenticated } = useAuth();
  const { error: showError, success: showSuccess } = useToast();

  const fetchPosts = useCallback(async (page = 0) => {
    try {
      setLoading(true);
      setError(null);
      const response = await postsAPI.getAll({
        params: {
          page,
          size: 9,
          sortBy: 'createdAt',
          sortDir: 'desc'
        }
      });
      
      const data = response.data;
      setPosts(data.content || []);
      // Calculate totalPages from total and pageSize
      const pageSize = 9;
      setTotalPages(Math.ceil((data.total || 0) / pageSize));
      setTotalElements(data.total || 0);
      setCurrentPage(page);
    } catch (err) {
      console.error('Error fetching posts:', err);
      setError('Failed to load posts. Please try again.');
      showError('Failed to load posts');
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < totalPages) {
      fetchPosts(newPage);
    }
  };

  const handleClaim = async (postId) => {
    if (!isAuthenticated) {
      showError('Please login to claim posts');
      return;
    }

    try {
      setClaimingPosts(prev => new Set(prev).add(postId));
      await claimsAPI.claim(postId);
      
      // Update the post in the local state
      setPosts(prevPosts => 
        prevPosts.map(post => 
          post.id === postId 
            ? { 
                ...post, 
                claimed: true, 
                claimedBy: user.username,
                currentClaims: (post.currentClaims || 0) + 1
              }
            : post
        )
      );
      
      showSuccess('Post claimed successfully!');
    } catch (err) {
      console.error('Error claiming post:', err);
      showError('Failed to claim post');
    } finally {
      setClaimingPosts(prev => {
        const newSet = new Set(prev);
        newSet.delete(postId);
        return newSet;
      });
    }
  };

  const handleUnclaim = async (postId) => {
    try {
      setClaimingPosts(prev => new Set(prev).add(postId));
      await claimsAPI.unclaim(postId);
      
      // Update the post in the local state
      setPosts(prevPosts => 
        prevPosts.map(post => 
          post.id === postId 
            ? { 
                ...post, 
                claimed: false, 
                claimedBy: null,
                currentClaims: Math.max((post.currentClaims || 1) - 1, 0)
              }
            : post
        )
      );
      
      showSuccess('Post unclaimed successfully!');
    } catch (err) {
      console.error('Error unclaiming post:', err);
      showError('Failed to unclaim post');
    } finally {
      setClaimingPosts(prev => {
        const newSet = new Set(prev);
        newSet.delete(postId);
        return newSet;
      });
    }
  };

  const getPostImage = (post) => {
    // Check if post has images array (new API structure)
    if (post.images && post.images.length > 0) {
      return postsAPI.getImage(post.images[0]); // Use first image
    }
    // Fallback to old imageFileName property
    if (post.imageFileName) {
      return postsAPI.getImage(post.imageFileName);
    }
    // Use local placeholder image
    return '/placeholder.png';
  };

  // Add helper functions for quota and status
  const getAvailabilityStatus = (post) => {
    const now = new Date();
    const openAt = post.openAt ? new Date(post.openAt) : null;
    const closeAt = post.closeAt ? new Date(post.closeAt) : null;
    const currentClaims = post.currentClaims || 0;
    const quota = post.quota || 0;

    // Check if dates are valid
    if (openAt && !isNaN(openAt.getTime()) && now < openAt) {
      return { status: 'upcoming', label: 'Upcoming', variant: 'gray' };
    }
    if (closeAt && !isNaN(closeAt.getTime()) && now > closeAt) {
      return { status: 'closed', label: 'Closed', variant: 'danger' };
    }
    if (quota > 0 && currentClaims >= quota) {
      return { status: 'full', label: 'Full', variant: 'warning' };
    }
    return { status: 'available', label: 'Available', variant: 'success' };
  };

  const getQuotaInfo = (post) => {
    const currentClaims = post.currentClaims || 0;
    const quota = post.quota || 0;
    return { current: currentClaims, total: quota, remaining: quota - currentClaims };
  };

  const canClaim = (post) => {
    const status = getAvailabilityStatus(post);
    return status.status === 'available' && !post.claimed;
  };

  const generatePaginationNumbers = () => {
    const pages = [];
    const maxVisiblePages = 5;
    
    if (totalPages <= maxVisiblePages) {
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      const startPage = Math.max(0, currentPage - 2);
      const endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
      
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
    }
    
    return pages;
  };

  if (loading && posts.length === 0) {
    return (
      <div className="flex justify-center items-center py-xl">
        <Spinner className="spinner-lg" />
        <span className="ml-md text-gray-600">Loading posts...</span>
      </div>
    );
  }

  if (error && posts.length === 0) {
    return (
      <div className="text-center py-xl">
        <div className="text-gray-500 mb-md">
          <svg className="w-16 h-16 mx-auto mb-md opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-lg font-medium text-gray-900 mb-sm">Oops! Something went wrong</p>
          <p className="text-gray-600 mb-lg">{error}</p>
        </div>
        <Button variant="primary" onClick={() => fetchPosts(currentPage)}>
          Try Again
        </Button>
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div className="text-center py-xl">
        <div className="text-gray-500 mb-md">
          <svg className="w-16 h-16 mx-auto mb-md opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2 2v-5m16 0h-2M4 13h2m13-8V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v1m-6 0V4a1 1 0 00-1-1H2a1 1 0 00-1 1v1m0 9h20" />
          </svg>
          <p className="text-lg font-medium text-gray-900 mb-sm">No posts available</p>
          <p className="text-gray-600">Be the first to share something with the community!</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-lg">
      {/* Results summary */}
      <div className="flex justify-between items-center text-sm text-gray-600">
        <span>
          Showing {currentPage * 9 + 1}-{Math.min((currentPage + 1) * 9, totalElements)} of {totalElements} posts
        </span>
        {loading && (
          <div className="flex items-center">
            <Spinner className="spinner-sm mr-sm" />
            <span>Updating...</span>
          </div>
        )}
      </div>

      {/* Posts grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-lg">
        {posts.map((post) => (
          <Card key={post.id} className="card-glass overflow-hidden">
            {/* Image */}
            <div className="relative h-48 bg-gray-100 overflow-hidden">
              <img
                src={getPostImage(post)}
                alt={post.title}
                className="w-full h-full object-cover transition-transform duration-300 hover:scale-105 card-image"
                onError={(e) => {
                  e.target.src = '/placeholder.png';
                }}
                loading="lazy"
              />
              
              {/* Status and quota badges */}
              <div className="absolute top-sm right-sm flex flex-col gap-1">
                {/* Availability status badge */}
                <Badge variant={getAvailabilityStatus(post).variant}>
                  {getAvailabilityStatus(post).label}
                </Badge>
                
                {/* Quota badge */}
                {post.quota && (
                  <Badge variant="gray" className="badge-quota">
                    {getQuotaInfo(post).current}/{post.quota}
                  </Badge>
                )}
              </div>
              
              {/* Tags badges */}
              {post.tags && post.tags.length > 0 && (
                <div className="absolute top-sm left-sm flex flex-wrap gap-1 max-w-[60%]">
                  {post.tags.slice(0, 2).map((tag, index) => (
                    <Badge key={index} variant="primary" className="text-xs">
                      {tag}
                    </Badge>
                  ))}
                  {post.tags.length > 2 && (
                    <Badge variant="gray" className="text-xs">
                      +{post.tags.length - 2}
                    </Badge>
                  )}
                </div>
              )}
            </div>

            {/* Content */}
            <div className="card-body">
              <h3 className="font-semibold text-lg text-gray-900 mb-sm line-clamp-2">
                {post.title}
              </h3>
              
              <p className="text-gray-600 text-sm mb-md line-clamp-3">
                {post.description}
              </p>

              {/* Post meta with quota info */}
              <div className="flex items-center justify-between text-xs text-gray-500 mb-md">
                <span>By {post.authorName || 'Anonymous'}</span>
                <div className="flex items-center gap-2">
                  {post.quota && (
                    <span className="text-primary font-medium">
                      {getQuotaInfo(post).remaining} left
                    </span>
                  )}
                  <span>{formatDate(post.createdAt || new Date().toISOString())}</span>
                </div>
              </div>

              {/* Open/Close times */}
              {(post.openAt || post.closeAt) && (
                <div className="text-xs text-gray-500 mb-md space-y-1">
                  {post.openAt && (
                    <div className="flex items-center">
                      <span className="w-12">Open:</span>
                      <span>{formatDate(post.openAt)}</span>
                    </div>
                  )}
                  {post.closeAt && (
                    <div className="flex items-center">
                      <span className="w-12">Close:</span>
                      <span>{formatDate(post.closeAt)}</span>
                    </div>
                  )}
                </div>
              )}

              {/* Location */}
              {post.location && (
                <div className="flex items-center text-sm text-gray-600 mb-md">
                  <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <span className="truncate">{post.location}</span>
                </div>
              )}

              {/* Claimed by */}
              {post.claimed && post.claimedBy && (
                <div className="flex items-center text-sm text-green-600 mb-md">
                  <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                  </svg>
                  <span>Claimed by {post.claimedBy}</span>
                </div>
              )}
            </div>

            {/* Actions */}
            {isAuthenticated && (
              <div className="card-footer">
                {(() => {
                  const status = getAvailabilityStatus(post);
                  const isClaimable = canClaim(post);
                  
                  if (status.status === 'upcoming') {
                    return (
                      <Button variant="ghost" size="sm" className="w-full" disabled>
                        Opens {formatDate(post.openAt || new Date().toISOString())}
                      </Button>
                    );
                  }
                  
                  if (status.status === 'closed') {
                    return (
                      <Button variant="ghost" size="sm" className="w-full" disabled>
                        Closed
                      </Button>
                    );
                  }
                  
                  if (status.status === 'full') {
                    return (
                      <Button variant="ghost" size="sm" className="w-full" disabled>
                        Quota Full
                      </Button>
                    );
                  }
                  
                  if (post.claimed && post.claimedBy === user?.username) {
                    return (
                      <Button
                        variant="warning"
                        size="sm"
                        className="w-full"
                        onClick={() => handleUnclaim(post.id)}
                        disabled={claimingPosts.has(post.id)}
                      >
                        {claimingPosts.has(post.id) ? (
                          <>
                            <Spinner className="spinner-sm" />
                            Unclaiming...
                          </>
                        ) : (
                          'Unclaim'
                        )}
                      </Button>
                    );
                  }
                  
                  if (post.claimed) {
                    return (
                      <Button variant="ghost" size="sm" className="w-full" disabled>
                        Already Claimed
                      </Button>
                    );
                  }
                  
                  if (isClaimable) {
                    return (
                      <Button
                        variant="primary"
                        size="sm"
                        className="w-full"
                        onClick={() => handleClaim(post.id)}
                        disabled={claimingPosts.has(post.id)}
                      >
                        {claimingPosts.has(post.id) ? (
                          <>
                            <Spinner className="spinner-sm" />
                            Claiming...
                          </>
                        ) : (
                          `Claim (${getQuotaInfo(post).remaining} left)`
                        )}
                      </Button>
                    );
                  }
                  
                  return (
                    <Button variant="ghost" size="sm" className="w-full" disabled>
                      Not Available
                    </Button>
                  );
                })()}
              </div>
            )}
          </Card>
        ))}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center space-x-sm mt-xl">
          {/* Previous button */}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 0 || loading}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Previous
          </Button>

          {/* Page numbers */}
          <div className="flex space-x-1">
            {generatePaginationNumbers().map((pageNum) => (
              <Button
                key={pageNum}
                variant={pageNum === currentPage ? "primary" : "ghost"}
                size="sm"
                className="min-w-[40px]"
                onClick={() => handlePageChange(pageNum)}
                disabled={loading}
              >
                {pageNum + 1}
              </Button>
            ))}
          </div>

          {/* Next button */}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1 || loading}
          >
            Next
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </Button>
        </div>
      )}
    </div>
  );
};

export default PostList;