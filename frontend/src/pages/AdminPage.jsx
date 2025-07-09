import { ShieldCheck, Trash2, UserMinus } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import Badge from '../components/Badge';
import Button from '../components/Button';
import Card from '../components/Card';
import Modal from '../components/Modal';
import Spinner from '../components/Spinner';
import { useToast } from '../hooks/useToast';
import { adminAPI } from '../lib/api';

const AdminPage = () => {
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(null);
  const [deleteModal, setDeleteModal] = useState({ open: false, postId: null, title: '' });
  const [removeClaimModal, setRemoveClaimModal] = useState({ 
    open: false, 
    postId: null, 
    userId: null, 
    userName: '',
    postTitle: '' 
  });
  const { success, error } = useToast();
  const fetchPosts = useCallback(async () => {
    try {
      const response = await adminAPI.getAllPosts();
      setPosts(response.data);
    } catch {
      error('Failed to fetch posts');
    } finally {
      setLoading(false);
    }
  }, [error]);

  useEffect(() => {
    fetchPosts();
  }, [fetchPosts]);

  const handleDeletePost = async () => {
    setActionLoading('delete');
    try {
      await adminAPI.deletePost(deleteModal.postId);
      success('Post deleted successfully');
      setDeleteModal({ open: false, postId: null, title: '' });
      await fetchPosts();
    } catch {
      error('Failed to delete post');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemoveClaim = async () => {
    setActionLoading('removeClaim');
    try {
      await adminAPI.removeClaim(removeClaimModal.postId, removeClaimModal.userId);
      success('Claim removed successfully');
      setRemoveClaimModal({ 
        open: false, 
        postId: null, 
        userId: null, 
        userName: '',
        postTitle: '' 
      });
      await fetchPosts();
    } catch {
      error('Failed to remove claim');
    } finally {
      setActionLoading(null);
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center py-xl">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="text-center mb-xl">
        <div className="w-16 h-16 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-xl flex items-center justify-center mx-auto mb-md">
          <ShieldCheck className="text-white" size={24} />
        </div>
        <h1 className="text-3xl font-bold mb-sm">
          <span className="gradient-text">Admin Dashboard</span>
        </h1>
        <p className="text-gray-600">
          Manage posts and claims across the platform
        </p>
      </div>

      <div className="space-y-lg">
        {posts.length === 0 ? (
          <div className="text-center py-xl">
            <p className="text-gray-500">No posts found</p>
          </div>
        ) : (
          posts.map((post) => (
            <Card key={post.id}>
              <Card.Body>
                <div className="flex items-start justify-between mb-md">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900 mb-sm">
                      {post.title}
                    </h3>
                    <p className="text-gray-600 text-sm mb-md">
                      {post.description}
                    </p>
                    <div className="flex items-center gap-md text-sm text-gray-500">
                      <span><strong>Author:</strong> {post.author}</span>
                      <span><strong>Created:</strong> {formatDate(post.createdAt)}</span>
                      {post.updatedAt && post.updatedAt !== post.createdAt && (
                        <span><strong>Updated:</strong> {formatDate(post.updatedAt)}</span>
                      )}
                    </div>
                  </div>
                  <div className="flex flex-col gap-sm">
                    {post.claimedBy && post.claimedBy.length > 0 ? (
                      <Badge variant="success">Claimed</Badge>
                    ) : (
                      <Badge variant="gray">Available</Badge>
                    )}
                  </div>
                </div>

                {/* Claims Section */}
                {post.claimedBy && post.claimedBy.length > 0 && (
                  <div className="bg-green-50 border border-green-200 rounded-lg p-md mb-md">
                    <h4 className="font-medium text-green-800 mb-sm">Claims:</h4>
                    <div className="space-y-sm">
                      {post.claimedBy.map((claimer, index) => (
                        <div key={index} className="flex items-center justify-between bg-white rounded p-sm">
                          <span className="text-sm text-green-700">{claimer}</span>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() => setRemoveClaimModal({
                              open: true,
                              postId: post.id,
                              userId: claimer, // Assuming claimer is the userId
                              userName: claimer,
                              postTitle: post.title
                            })}
                          >
                            <UserMinus size={14} />
                            Remove
                          </Button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Images Section */}
                {post.images && post.images.length > 0 && (
                  <div className="mb-md">
                    <h4 className="font-medium text-gray-700 mb-sm">Images:</h4>
                    <div className="flex gap-sm">
                      {post.images.slice(0, 3).map((image, index) => (
                        <div key={index} className="w-16 h-16 bg-gray-100 rounded border overflow-hidden">
                          <img
                            src={`http://localhost:8080/api/posts/images/${image}`}
                            alt={`${post.title} ${index + 1}`}
                            className="w-full h-full object-cover"
                          />
                        </div>
                      ))}
                      {post.images.length > 3 && (
                        <div className="w-16 h-16 bg-gray-100 rounded border flex items-center justify-center text-xs text-gray-500">
                          +{post.images.length - 3}
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Actions */}
                <div className="flex justify-end">
                  <Button
                    variant="danger"
                    size="sm"
                    onClick={() => setDeleteModal({
                      open: true,
                      postId: post.id,
                      title: post.title
                    })}
                  >
                    <Trash2 size={14} />
                    Delete Post
                  </Button>
                </div>
              </Card.Body>
            </Card>
          ))
        )}
      </div>

      {/* Delete Post Modal */}
      <Modal
        isOpen={deleteModal.open}
        onClose={() => setDeleteModal({ open: false, postId: null, title: '' })}
        title="Delete Post"
      >
        <div className="mb-lg">
          <p className="text-gray-600">
            Are you sure you want to delete the post "{deleteModal.title}"? 
            This action cannot be undone.
          </p>
        </div>
        <Modal.Footer>
          <Button
            variant="ghost"
            onClick={() => setDeleteModal({ open: false, postId: null, title: '' })}
          >
            Cancel
          </Button>
          <Button
            variant="danger"
            onClick={handleDeletePost}
            loading={actionLoading === 'delete'}
          >
            Delete Post
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Remove Claim Modal */}
      <Modal
        isOpen={removeClaimModal.open}
        onClose={() => setRemoveClaimModal({ 
          open: false, 
          postId: null, 
          userId: null, 
          userName: '',
          postTitle: '' 
        })}
        title="Remove Claim"
      >
        <div className="mb-lg">
          <p className="text-gray-600">
            Are you sure you want to remove {removeClaimModal.userName}'s claim 
            on "{removeClaimModal.postTitle}"?
          </p>
        </div>
        <Modal.Footer>
          <Button
            variant="ghost"
            onClick={() => setRemoveClaimModal({ 
              open: false, 
              postId: null, 
              userId: null, 
              userName: '',
              postTitle: '' 
            })}
          >
            Cancel
          </Button>
          <Button
            variant="warning"
            onClick={handleRemoveClaim}
            loading={actionLoading === 'removeClaim'}
          >
            Remove Claim
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default AdminPage;
