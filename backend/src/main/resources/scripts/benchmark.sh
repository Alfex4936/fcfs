redis-cli -c -h 127.0.0.1 -p 9001 -a myredispassword DEL post:{42}:claimants post:{42}:claims_count

for port in 9001 9002 9003; do
  redis-cli -h 127.0.0.1 -p $port -a myredispassword SCRIPT LOAD "$(cat claim1.lua)"
done

for port in 9001 9002 9003; do
  redis-cli -h 127.0.0.1 -p $port -a myredispassword SCRIPT LOAD "$(cat claim2.lua)"
done

time seq 1 500 | parallel -j100 \
  'redis-cli -c -h 127.0.0.1 -p 9001 -a myredispassword \
  EVALSHA 77ceff856d55ff73850b49d5c345afdb158fe8b1 1 post:{42} user_{#} 100' > claim1.log


time seq 1 500 | parallel -j100 \
  'redis-cli -c -h 127.0.0.1 -p 9001 -a myredispassword \
  EVALSHA 69427c01fe2fe00890105154f05711e82c7bbb73 1 post:{42} user_{#} 100' > claim2.log

# or memtier-benchmark (supports cluster)