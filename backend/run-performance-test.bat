@echo off
echo Running high-performance benchmark tests...

REM JVM tuning for high throughput
set JAVA_OPTS=-Xms4g -Xmx8g ^
-XX:+UseG1GC ^
-XX:MaxGCPauseMillis=50 ^
-XX:+UseStringDeduplication ^
-XX:+OptimizeStringConcat ^
-server ^
-XX:+AggressiveOpts

echo Using JVM options: %JAVA_OPTS%

REM Run the specific extreme load test
gradlew.bat test --tests "*ConcurrentClaimBenchmarkTest.benchmarkConcurrentClaims_ExtremeLoad" -Dspring.profiles.active=test

echo Performance test completed!
pause
