set -e

# Quick HTTP check with minimal timeout
if command -v curl >/dev/null 2>&1; then
    # Use curl with minimal options
    curl -sf --max-time 5 --connect-timeout 2 http://localhost:9090/health >/dev/null 2>&1
    HEALTH_STATUS=$?
else
    # Fallback to python if curl not available
    python3 -c "
import urllib.request
import urllib.error
import socket

try:
    socket.setdefaulttimeout(3)
    urllib.request.urlopen('http://localhost:9090/health', timeout=3)
    exit(0)
except:
    exit(1)
" >/dev/null 2>&1
    HEALTH_STATUS=$?
fi

if [ $HEALTH_STATUS -ne 0 ]; then
    echo "Health check failed: HTTP server not responding"
    exit 1
fi

# Quick memory check - only if we have enough free memory
if [ -f /proc/meminfo ]; then
    AVAILABLE_KB=$(awk '/MemAvailable:/ {print $2}' /proc/meminfo 2>/dev/null || echo "999999")
    if [ "$AVAILABLE_KB" -lt 50000 ]; then  # Less than 50MB available
        echo "Health check warning: Low memory ($((AVAILABLE_KB/1024))MB available)"
        # Don't fail, just warn
    fi
fi

# Quick disk check for temp directory
if [ -d "/app/temp" ]; then
    TEMP_USAGE=$(du -sm /app/temp 2>/dev/null | cut -f1 || echo "0")
    if [ "$TEMP_USAGE" -gt 100 ]; then  # More than 100MB in temp
        echo "Health check warning: Large temp usage (${TEMP_USAGE}MB)"
        # Cleanup old files
        find /app/temp -type f -mmin +30 -delete 2>/dev/null || true
    fi
fi

echo "Health check passed"
exit 0
