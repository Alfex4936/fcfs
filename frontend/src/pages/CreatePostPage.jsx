import { Plus, Upload, X } from 'lucide-react';
import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../components/Button';
import Card from '../components/Card';
import Input from '../components/Input';
import Textarea from '../components/Textarea';
import { useToast } from '../hooks/useToast';
import { postsAPI } from '../lib/api';

const CreatePostPage = () => {
  // Helper function to format date for datetime-local input (keeps local timezone)
  const formatForDateTimeLocal = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  // Set default times in user's local timezone
  const now = new Date();
  const tenMinutesLater = new Date(now.getTime() + 10 * 60 * 1000); // +10 minutes
  const oneHourTenMinutesLater = new Date(now.getTime() + 70 * 60 * 1000); // +1 hour +10 minutes
  
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    quota: 1,
    openAt: formatForDateTimeLocal(tenMinutesLater), // +10 minutes from now in local time
    closeAt: formatForDateTimeLocal(oneHourTenMinutesLater), // +1 hour +10 minutes from now in local time
    tags: []
  });
  const [tagInput, setTagInput] = useState('');
  const [images, setImages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef(null);
  const navigate = useNavigate();
  const { success, error } = useToast();
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'quota' ? parseInt(value) || 1 : value
    }));
  };

  const addTag = () => {
    const tag = tagInput.trim();
    if (tag && !formData.tags.includes(tag) && formData.tags.length < 5) {
      setFormData(prev => ({
        ...prev,
        tags: [...prev.tags, tag]
      }));
      setTagInput('');
    }
  };

  const removeTag = (tagToRemove) => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags.filter(tag => tag !== tagToRemove)
    }));
  };

  const handleTagKeyPress = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag();
    }
  };

  const handleFileSelect = (files) => {
    const validFiles = Array.from(files).filter(file => {
      if (!file.type.startsWith('image/')) {
        error('Please select only image files');
        return false;
      }
      if (file.size > 5 * 1024 * 1024) { // 5MB limit
        error('File size must be less than 5MB');
        return false;
      }
      return true;
    });

    if (images.length + validFiles.length > 5) {
      error('Maximum 5 images allowed');
      return;
    }

    setImages(prev => [...prev, ...validFiles]);
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelect(e.dataTransfer.files);
    }
  };

  const removeImage = (index) => {
    setImages(prev => prev.filter((_, i) => i !== index));
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.title.trim()) {
      error('Title is required');
      return;
    }
    
    if (!formData.description.trim()) {
      error('Description is required');
      return;
    }

    if (formData.quota < 1) {
      error('Quota must be at least 1');
      return;
    }

    if (!formData.openAt) {
      error('Open time is required');
      return;
    }

    if (!formData.closeAt) {
      error('Close time is required');
      return;
    }    const openTime = new Date(formData.openAt);
    const closeTime = new Date(formData.closeAt);
    const now = new Date();

    // Allow open time to be at least 5 minutes in the future
    if (openTime < new Date(now.getTime() + 5 * 60 * 1000)) {
      error('Open time must be at least 5 minutes in the future');
      return;
    }

    if (closeTime <= openTime) {
      error('Close time must be after open time');
      return;
    }

    setLoading(true);    try {
      const submitData = new FormData();
      
      // Convert datetime-local to UTC for backend
      const convertToUTC = (dateTimeString) => {
        // datetime-local gives us a string like "2025-06-20T20:49"
        // We create a Date object which treats this as local time
        const localDate = new Date(dateTimeString);
        // toISOString() converts to UTC automatically
        return localDate.toISOString();
      };
      
      const postData = {
        ...formData,
        openAt: convertToUTC(formData.openAt),
        closeAt: convertToUTC(formData.closeAt)
      };
      
      // Create a Blob with proper JSON content type for the post data
      const postBlob = new Blob([JSON.stringify(postData)], {
        type: 'application/json'
      });
      submitData.append('post', postBlob);
      
      if (images.length > 0) {
        images.forEach(image => {
          submitData.append('images', image);
        });
      } else {
        // Backend expects images array, so send empty file if no images
        submitData.append('images', new File([], 'empty', { type: 'application/octet-stream' }));
      }

      await postsAPI.create(submitData);
      success('Post created successfully!');
      navigate('/');
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to create post';
      error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div className="text-center mb-xl">
        <div className="w-16 h-16 bg-gradient-to-br from-green-500 to-teal-600 rounded-xl flex items-center justify-center mx-auto mb-md">
          <Plus className="text-white" size={24} />
        </div>
        <h1 className="text-3xl font-bold mb-sm">
          <span className="gradient-text">Share Something</span>
        </h1>
        <p className="text-gray-600">
          Share an item with the community using the first-come-first-serve principle
        </p>
      </div>

      <Card>
        <form onSubmit={handleSubmit}>          <Card.Body className="space-y-lg">
            <Input
              name="title"
              label="Title"
              placeholder="What are you sharing?"
              value={formData.title}
              onChange={handleInputChange}
              required
            />

            <Textarea
              name="description"
              label="Description"
              placeholder="Describe the item, condition, pickup details, etc."
              value={formData.description}
              onChange={handleInputChange}
              required
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
              <Input
                name="quota"
                label="Quota"
                type="number"
                min="1"
                placeholder="How many people can claim this?"
                value={formData.quota}
                onChange={handleInputChange}
                required
              />
              <div>
                <label className="input-label">Tags (Optional)</label>
                <div className="flex gap-sm">
                  <input
                    type="text"
                    className="input flex-1"
                    placeholder="Add a tag..."
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyPress={handleTagKeyPress}
                    maxLength={20}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={addTag}
                    disabled={!tagInput.trim() || formData.tags.length >= 5}
                  >
                    Add
                  </Button>
                </div>
                {formData.tags.length > 0 && (
                  <div className="flex flex-wrap gap-xs mt-sm">
                    {formData.tags.map((tag, index) => (
                      <span
                        key={index}
                        className="inline-flex items-center gap-xs px-sm py-xs bg-blue-100 text-blue-800 rounded-md text-sm"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => removeTag(tag)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          <X size={12} />
                        </button>
                      </span>
                    ))}
                  </div>
                )}
                <p className="text-xs text-gray-500 mt-xs">
                  Add up to 5 tags to help people find your post
                </p>
              </div>
            </div>            <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
              <Input
                name="openAt"
                label="Open Time"
                type="datetime-local"
                value={formData.openAt}
                onChange={handleInputChange}
                required
                min={formatForDateTimeLocal(new Date(Date.now() + 5 * 60 * 1000))} // 5 minutes from now in local time
              />
              <Input
                name="closeAt"
                label="Close Time"
                type="datetime-local"
                value={formData.closeAt}
                onChange={handleInputChange}
                required
                min={formData.openAt}
              />
            </div>

            <div className="text-sm text-gray-600 bg-blue-50 p-md rounded-lg">
              <p className="font-medium mb-xs">How it works:</p>
              <ul className="space-y-xs text-xs">
                <li>• People can claim your item starting at the <strong>Open Time</strong></li>
                <li>• Claims are processed on a first-come-first-serve basis</li>
                <li>• No more claims accepted after the <strong>Close Time</strong></li>
                <li>• Up to <strong>{formData.quota}</strong> {formData.quota === 1 ? 'person' : 'people'} can claim this item</li>
              </ul>
            </div>

            <div>
              <label className="input-label">Images (Optional)</label>
              <div
                className={`image-upload ${dragActive ? 'dragover' : ''}`}
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
              >
                <div className="text-center">
                  <Upload className="mx-auto text-gray-400 mb-md" size={32} />
                  <p className="text-gray-600 mb-sm">
                    Click to upload or drag and drop
                  </p>
                  <p className="text-sm text-gray-500">
                    PNG, JPG, GIF up to 5MB each (max 5 images)
                  </p>
                </div>
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  accept="image/*"
                  className="hidden"
                  onChange={(e) => handleFileSelect(e.target.files)}
                />
              </div>

              {images.length > 0 && (
                <div className="image-preview">
                  {images.map((image, index) => (
                    <div key={index} className="image-preview-item">
                      <img
                        src={URL.createObjectURL(image)}
                        alt={`Preview ${index + 1}`}
                      />
                      <button
                        type="button"
                        onClick={() => removeImage(index)}
                        className="image-preview-remove"
                      >
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </Card.Body>

          <Card.Footer className="flex gap-md">
            <Button
              type="button"
              variant="ghost"
              onClick={() => navigate('/')}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={loading}
              className="flex-1"
            >
              <Plus size={16} />
              Create Post
            </Button>
          </Card.Footer>
        </form>
      </Card>
    </div>
  );
};

export default CreatePostPage;
