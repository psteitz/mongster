import { useState, useEffect } from 'react';
import { messagesApi, clearApi } from './api-provider.jsx';

// App component - main container for Mongster message viewer
function App() {
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isClearing, setIsClearing] = useState(false);

  // Load messages on mount and refresh periodically (every 30 seconds like the Vue app)
  useEffect(() => {
    loadMessages();
    
    // Auto-refresh every 30 seconds, matching original behavior
    const interval = setInterval(loadMessages, 30000);
    
    return () => clearInterval(interval);
  }, []);

  const loadMessages = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await messagesApi.getAll();
      setMessages(data || []);
    } catch (err) {
      console.error('Failed to load messages:', err);
      setError(err.message || 'Failed to load messages');
    } finally {
      setLoading(false);
    }
  };

  const handleClear = async () => {
    try {
      setIsClearing(true);
      await clearApi.clear();
      console.log("Cleared");
      await loadMessages();
    } catch (err) {
      console.error('Failed to clear messages:', err);
      setError(err.message || 'Failed to clear messages');
    } finally {
      setIsClearing(false);
    }
  };

  if (loading) {
    return <div className="loading">Loading messages...</div>;
  }

  if (error) {
    return (
      <div className="error" style={{ color: 'red', padding: '20px' }}>
        Error: {error}
      </div>
    );
  }

  return (
    <div className="app">
      <h1>MongsterView - Messages Sent</h1>
      
      {messages.length === 0 && !loading ? (
        <div className="no-messages">No messages to display</div>
      ) : (
        <div className="table-responsive">
          <table className="table table-hover">
            <thead>
              <tr>
                <th>Date</th>
                <th>Subject</th>
                <th>To</th>
                <th>Cc</th>
                <th>From</th>
                <th>ReplyTo</th>
                <th>Message body</th>
              </tr>
            </thead>
            <tbody>
              {messages.map((message, index) => (
                <tr key={message._id || message.id || `msg-${index}`}>
                  <td>{formatDate(message.date)}</td>
                  <td>{message.subject || '-'}</td>
                  <td>
                    {(message.to || []).map((to, i) => to).join(' ')}
                  </td>
                  <td>
                    {(message.cc || []).map((cc, i) => cc).join(' ')}
                  </td>
                  <td>{message.from}</td>
                  <td>{message.replyto}</td>
                  <td>{message.body}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <form className="clear-form" onSubmit={handleClear}>
        <button 
          type="submit" 
          disabled={isClearing || loading}
          className="btn btn-primary"
        >
          {isClearing ? 'Clearing...' : 'Clear'}
        </button>
      </form>
    </div>
  );
}

// Format date from MongoDB format (ISO string) to display
function formatDate(dateString) {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    return date.toLocaleString();
  } catch {
    return dateString;
  }
}

export default App;
