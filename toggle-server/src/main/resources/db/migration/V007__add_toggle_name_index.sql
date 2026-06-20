-- Add index on toggle_name for query optimization
-- Reduces O(n) full table scan to O(log n) in findByToggleName operations
CREATE INDEX idx_client_toggle_subscription_toggle_name 
  ON client_toggle_subscription(toggle_name);
