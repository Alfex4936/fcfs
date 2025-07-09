#!/bin/bash

BASE_DIR="/mnt/d/Dev/Java/first-come-first-serve/redis/redis-nodes"
declare -A PIDS

start_node() {
    port="$1"
    cd "$BASE_DIR/$port" || exit 1
    redis-server redis.conf > "redis-$port.log" 2>&1 &
    pid=$!
    PIDS["$port"]=$pid
}

kill_node() {
    port="$1"
    pid="${PIDS[$port]}"
    if kill -0 "$pid" 2>/dev/null; then
        kill "$pid"
        unset PIDS["$port"]
    fi
}

status_node() {
    port="$1"
    pid="${PIDS[$port]}"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        # Query memory & CPU
        stats=$(ps -p "$pid" -o %cpu=,%mem=,rss=)
        cpu=$(awk '{print $1}' <<< "$stats")
        mem=$(awk '{print $2}' <<< "$stats")
        rss=$(awk '{printf "%.1f", $3/1024}' <<< "$stats")  # KB -> MB
        printf "✅ %-5s (PID %5d) CPU: %5s%% MEM: %5s%% RSS: %5s MB\n" "$port" "$pid" "$cpu" "$mem" "$rss"
    else
        printf "❌ %-5s (not running)\n" "$port"
    fi
}

# Start all nodes initially
for port in {9001..9008}; do
    start_node "$port"
done

while true; do
    echo
    echo "==== Redis Nodes Status ===="
    idx=1
    declare -A IDX2PORT
    for port in {9001..9008}; do
        status_node "$port"
        IDX2PORT["$idx"]=$port
        ((idx++))
    done

    echo
    echo "Commands: k<num>=kill, r<num>=restart, q=quit"
    read -rp "> " cmd

    case "$cmd" in
        k[1-8])
            num="${cmd:1:1}"
            port="${IDX2PORT[$num]}"
            echo "Killing $port..."
            kill_node "$port"
            ;;
        r[1-8])
            num="${cmd:1:1}"
            port="${IDX2PORT[$num]}"
            echo "Restarting $port..."
            kill_node "$port"
            sleep 1
            start_node "$port"
            ;;
        q)
            echo "Quitting. Killing all nodes..."
            for port in {9001..9008}; do
                kill_node "$port"
            done
            exit 0
            ;;
        *)
            echo "Unknown command."
            ;;
    esac
done

