@echo off
echo Running ultra-high-performance benchmark tests...

REM JVM tuning for maximum throughput
set JAVA_OPTS=-Xms8g -Xmx16g ^
-XX:+UseG1GC ^
-XX:MaxGCPauseMillis=20 ^
-XX:+UseStringDeduplication ^
-XX:+OptimizeStringConcat ^
-XX:+UseCompressedOops ^
-XX:+AggressiveOpts ^
-XX:+UseFastAccessorMethods ^
-server

echo Using JVM options: %JAVA_OPTS%

REM Run with performance profile
gradlew.bat test --tests "*ConcurrentClaimBenchmarkTest.benchmarkConcurrentClaims_ExtremeLoad" ^
-Dspring.profiles.active=test,performance ^
-Dlogging.level.com.zaxxer.hikari=DEBUG ^
-Dlogging.level.org.springframework.data.redis=DEBUG

echo Performance test completed!
echo Check logs for HikariCP and Redis connection pool usage
pause
