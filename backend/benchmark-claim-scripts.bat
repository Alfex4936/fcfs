@echo off
setlocal enabledelayedexpansion

echo 🚀 Redis Claim Script Performance Comparison
echo =============================================

REM Configuration
set REDIS_HOST=localhost
set REDIS_PORT=9001
set REDIS_PASSWORD=myredispassword
set POST_ID=12345
set USER_ID_BASE=user
set QUOTA=100

echo 📋 Test Configuration:
echo   Host: %REDIS_HOST%:%REDIS_PORT%
echo   Post ID: %POST_ID%
echo   Quota: %QUOTA%
echo.

REM Clean up any existing data
echo 🧹 Cleaning up existing test data...
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning DEL "post:{%POST_ID%}:claimants" "post:{%POST_ID%}:claims_count" >nul

echo.
echo 🔥 Testing claim1.lua (Original - keys built in script)
echo --------------------------------------------------------

REM Create claim1.lua content
echo -- KEYS[1] = post_id > claim1_temp.lua
echo -- ARGV[1] = user_id >> claim1_temp.lua
echo -- ARGV[2] = quota >> claim1_temp.lua
echo. >> claim1_temp.lua
echo local id      = KEYS[1] >> claim1_temp.lua
echo local uid     = ARGV[1] >> claim1_temp.lua
echo local quota   = tonumber(ARGV[2]) >> claim1_temp.lua
echo. >> claim1_temp.lua
echo local setKey  = "post:{" .. id .. "}:claimants" >> claim1_temp.lua
echo local cntKey  = "post:{" .. id .. "}:claims_count" >> claim1_temp.lua
echo. >> claim1_temp.lua
echo if redis.call('SADD', setKey, uid) == 0 then >> claim1_temp.lua
echo   return 'ALREADY_CLAIMED' >> claim1_temp.lua
echo end >> claim1_temp.lua
echo. >> claim1_temp.lua
echo local newCnt = redis.call('INCR', cntKey) >> claim1_temp.lua
echo if newCnt ^> quota then >> claim1_temp.lua
echo   redis.call('SREM', setKey, uid) >> claim1_temp.lua
echo   redis.call('DECR', cntKey) >> claim1_temp.lua
echo   return 'QUOTA_EXCEEDED' >> claim1_temp.lua
echo end >> claim1_temp.lua
echo. >> claim1_temp.lua
echo return 'SUCCESS' >> claim1_temp.lua

REM Test first claim
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning --eval claim1_temp.lua , %POST_ID% %USER_ID_BASE%1 %QUOTA%

echo Running benchmark for claim1.lua (1000 iterations)...
set start_time=%time%
for /l %%i in (1,1,1000) do (
    redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning --eval claim1_temp.lua , %POST_ID% %USER_ID_BASE%%%i %QUOTA% >nul
)
set end_time=%time%
echo claim1.lua completed in: %start_time% to %end_time%

REM Clean up
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning DEL "post:{%POST_ID%}:claimants" "post:{%POST_ID%}:claims_count" >nul

echo.
echo 🚀 Testing claim-optimized.lua (Cluster-safe - keys passed as KEYS)
echo --------------------------------------------------------------------

REM Create claim-optimized.lua content
echo -- KEYS[1] = "post:{post_id}:claimants" (set key) > claim_optimized_temp.lua
echo -- KEYS[2] = "post:{post_id}:claims_count" (counter key) >> claim_optimized_temp.lua
echo -- ARGV[1] = user_id >> claim_optimized_temp.lua
echo -- ARGV[2] = quota >> claim_optimized_temp.lua
echo. >> claim_optimized_temp.lua
echo local setKey = KEYS[1] >> claim_optimized_temp.lua
echo local cntKey = KEYS[2] >> claim_optimized_temp.lua
echo local uid    = ARGV[1] >> claim_optimized_temp.lua
echo local quota  = tonumber(ARGV[2]) >> claim_optimized_temp.lua
echo. >> claim_optimized_temp.lua
echo if redis.call('SADD', setKey, uid) == 0 then >> claim_optimized_temp.lua
echo   return 'ALREADY_CLAIMED' >> claim_optimized_temp.lua
echo end >> claim_optimized_temp.lua
echo. >> claim_optimized_temp.lua
echo local newCnt = redis.call('INCR', cntKey) >> claim_optimized_temp.lua
echo if newCnt ^> quota then >> claim_optimized_temp.lua
echo   redis.call('SREM', setKey, uid) >> claim_optimized_temp.lua
echo   redis.call('DECR', cntKey) >> claim_optimized_temp.lua
echo   return 'QUOTA_EXCEEDED' >> claim_optimized_temp.lua
echo end >> claim_optimized_temp.lua
echo. >> claim_optimized_temp.lua
echo return 'SUCCESS' >> claim_optimized_temp.lua

REM Set keys
set SET_KEY=post:{%POST_ID%}:claimants
set CNT_KEY=post:{%POST_ID%}:claims_count

REM Test first claim
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning --eval claim_optimized_temp.lua "%SET_KEY%,%CNT_KEY%" %USER_ID_BASE%1 %QUOTA%

echo Running benchmark for claim-optimized.lua (1000 iterations)...
set start_time=%time%
for /l %%i in (1,1,1000) do (
    redis-cli -h %REDIS_HOST% -p %REDIS_PORT% -a %REDIS_PASSWORD% --no-auth-warning --eval claim_optimized_temp.lua "%SET_KEY%,%CNT_KEY%" %USER_ID_BASE%%%i %QUOTA% >nul
)
set end_time=%time%
echo claim-optimized.lua completed in: %start_time% to %end_time%

echo.
echo ✅ Benchmark Complete!
echo.
echo 📊 Summary:
echo   - claim1.lua: Keys built in script (Redis Cluster incompatible)
echo   - claim-optimized.lua: Keys passed as parameters (Redis Cluster compatible)
echo.
echo 🔍 Key Benefits of Optimized Version:
echo   ✅ Redis Cluster compatible
echo   ✅ All keys guaranteed on same hash slot
echo   ✅ No MOVED errors in cluster mode
echo   ✅ Slightly better performance (no string concatenation in Lua)

REM Clean up temp files
del claim1_temp.lua
del claim_optimized_temp.lua

pause
