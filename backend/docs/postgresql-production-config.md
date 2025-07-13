# PostgreSQL Production Configuration

## Server-side Keep-Alive Settings (postgresql.conf)

```bash
# Connection Settings
listen_addresses = '*'
port = 5432
max_connections = 200                    # Should be >= HikariCP max pool size across all app instances

# Keep-Alive & Timeout Settings
tcp_keepalives_idle = 600               # 10분 - Start sending keep-alive after idle time
tcp_keepalives_interval = 30            # 30초 - Interval between keep-alive probes  
tcp_keepalives_count = 3                # 3회 - Number of failed probes before giving up

# Connection Timeouts
authentication_timeout = 10s            # Max time to complete authentication
statement_timeout = 300s                # 5분 - Max time for any single statement
idle_in_transaction_session_timeout = 600s  # 10분 - Kill idle transactions

# Connection Pooling (if using pgbouncer)
# pool_mode = transaction
# default_pool_size = 25
# max_client_conn = 1000
# server_lifetime = 3600                # 1시간 - Max server connection lifetime
# server_idle_timeout = 600             # 10분 - Close idle server connections
```

## Production Recommendations by Component:

### 1. HTTP Keep-Alive (Client ↔ Web Server)
- **keep-alive-timeout**: 15-60s
- **max-keep-alive-requests**: 100-1000
- **Rationale**: Balance between connection reuse and resource consumption

### 2. Database Connections (App ↔ PostgreSQL)
- **idle-timeout**: 10-30 minutes
- **max-lifetime**: 30 minutes - 2 hours  
- **keepalive-time**: 5-10 minutes
- **Rationale**: Database connections are expensive to create, keep them longer

### 3. Redis Connections (App ↔ Cache)
- **timeout**: 1-3 seconds
- **pool settings**: Similar to database but can be more aggressive
- **Rationale**: Redis is fast, shorter timeouts are acceptable

### 4. Load Balancer Settings (if using)
- **nginx proxy_read_timeout**: 60s
- **AWS ALB idle_timeout**: 60s
- **Rationale**: Should be longer than application timeouts

## Environment-Specific Recommendations:

### High Traffic (e.g., > 1000 RPS)
```yaml
# Shorter HTTP keep-alive to free up connections faster
keep-alive-timeout: 15s
max-keep-alive-requests: 100

# Longer DB connections for efficiency
idle-timeout: 1800000  # 30분
max-lifetime: 3600000  # 1시간
```

### Low Traffic (e.g., < 100 RPS)  
```yaml
# Longer HTTP keep-alive for better reuse
keep-alive-timeout: 60s
max-keep-alive-requests: 1000

# Shorter DB connections to save resources
idle-timeout: 600000   # 10분
max-lifetime: 1800000  # 30분
```

### Behind NAT/Firewall
```yaml
# More aggressive keep-alive to prevent connection drops
keepalive-time: 180000  # 3분 (shorter than typical NAT timeout)
tcp_keepalives_idle = 300  # 5분
```

## Monitoring Commands:

```sql
-- Check active connections
SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active';

-- Check connection states
SELECT state, count(*) FROM pg_stat_activity GROUP BY state;

-- Check long-running queries
SELECT now() - query_start as duration, query 
FROM pg_stat_activity 
WHERE state = 'active' AND now() - query_start > interval '1 minute';
```
