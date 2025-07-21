set -e

echo "Starting OPTIMIZED PrusaSlicer Container..."
echo "Container ID: ${CONTAINER_ID:-unknown}"
echo "Low Resource Mode: ${LOW_RESOURCE_MODE:-false}"
echo "Memory Limit: ${MEMORY_LIMIT_MB:-512}MB"

# Start virtual display with minimal resources
echo "Starting minimal X server..."
Xvfb :99 -screen 0 800x600x16 -nolisten tcp -noreset &  # Ridotta risoluzione e colori
XVFB_PID=$!
export DISPLAY=:99

# Wait for X server
sleep 1

# Verify PrusaSlicer (quick check)
echo "Quick PrusaSlicer verification..."
timeout 10s prusa-slicer --version > /dev/null 2>&1 || {
    echo "ERROR: PrusaSlicer not working"
    kill $XVFB_PID 2>/dev/null || true
    exit 1
}

# Create directories with size limits
mkdir -p "${SLICER_TEMP_DIR}" "${SLICER_OUTPUT_DIR}"

# Set resource limits if in low resource mode
if [ "${LOW_RESOURCE_MODE}" = "true" ]; then
    echo "Applying low resource optimizations..."
    
    # Limit temp directory size
    if command -v truncate >/dev/null 2>&1; then
        # Create dummy file to reserve space (prevents out of space)
        truncate -s 50M "${SLICER_TEMP_DIR}/.space_reserve" 2>/dev/null || true
    fi
    
    # Set process limits
    ulimit -v $((${MEMORY_LIMIT_MB:-512} * 1024)) 2>/dev/null || true  # Virtual memory limit
    ulimit -d $((${MEMORY_LIMIT_MB:-512} * 1024)) 2>/dev/null || true  # Data segment limit
fi

# Cleanup function
cleanup() {
    echo "Shutting down container..."
    kill $XVFB_PID 2>/dev/null || true
    
    # Quick cleanup
    rm -f "${SLICER_TEMP_DIR}"/.space_reserve 2>/dev/null || true
    find "${SLICER_TEMP_DIR}" -type f -mtime +0 -delete 2>/dev/null || true
    
    exit 0
}

trap cleanup SIGTERM SIGINT

# Start MINIMAL HTTP server for health checks and job receiving
echo "Starting minimal container management server..."
python3 -c "
import http.server
import socketserver
import json
import subprocess
import os
import sys
import threading
import time
from urllib.parse import urlparse

class MinimalSlicingHandler(http.server.SimpleHTTPRequestHandler):
    # Cache per evitare chiamate ripetute
    _version_cache = None
    _memory_cache = None
    _cache_time = 0
    
    def log_message(self, format, *args):
        # Disable logging in low resource mode
        if os.environ.get('LOW_RESOURCE_MODE') != 'true':
            super().log_message(format, *args)
    
    def do_GET(self):
        if self.path == '/health':
            self.send_health_response()
        elif self.path == '/status':
            self.send_status_response()
        else:
            self.send_response(404)
            self.end_headers()
    
    def send_health_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        
        # Use cached data to reduce resource usage
        current_time = time.time()
        if current_time - self._cache_time > 60:  # Cache for 1 minute
            self._version_cache = self.get_slicer_version()
            self._memory_cache = self.get_available_memory()
            self._cache_time = current_time
        
        response = {
            'status': 'healthy',
            'container_id': os.environ.get('CONTAINER_ID', 'unknown'),
            'prusaslicer_version': self._version_cache or 'unknown',
            'temp_dir': os.environ.get('SLICER_TEMP_DIR'),
            'output_dir': os.environ.get('SLICER_OUTPUT_DIR'),
            'available_memory': self._memory_cache or 0,
            'active_jobs': 0,  # Track in future
            'low_resource_mode': os.environ.get('LOW_RESOURCE_MODE') == 'true'
        }
        self.wfile.write(json.dumps(response, separators=(',', ':')).encode())
    
    def send_status_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        response = {
            'ready': True,
            'busy': False,
            'uptime': self.get_uptime(),
            'memory_limit': os.environ.get('MEMORY_LIMIT_MB', '512') + 'MB'
        }
        self.wfile.write(json.dumps(response, separators=(',', ':')).encode())
    
    def do_POST(self):
        if self.path == '/slice':
            content_length = int(self.headers['Content-Length'])
            # Limit POST size in low resource mode
            max_size = 50 * 1024 * 1024  # 50MB limit
            if content_length > max_size:
                self.send_response(413)  # Payload too large
                self.end_headers()
                return
            
            post_data = self.rfile.read(content_length)
            
            try:
                job_data = json.loads(post_data.decode('utf-8'))
                result = self.process_slicing_job(job_data)
                
                self.send_response(200 if result['success'] else 500)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps(result, separators=(',', ':')).encode())
                
            except Exception as e:
                self.send_response(500)
                self.send_header('Content-type', 'application/json')
                self.end_headers()
                error_response = {'success': False, 'error': str(e)}
                self.wfile.write(json.dumps(error_response, separators=(',', ':')).encode())
        else:
            self.send_response(404)
            self.end_headers()
    
    def process_slicing_job(self, job_data):
        '''Optimized slicing job processing'''
        try:
            job_id = job_data.get('job_id', 'unknown')
            print(f'Processing slicing job: {job_id}')
            
            # TODO: Implement optimized slicing logic
            # - Use temporary files efficiently
            # - Monitor memory usage
            # - Cleanup immediately after processing
            
            return {
                'success': True,
                'job_id': job_id,
                'gcode_data': 'placeholder_gcode_data',
                'metrics': {
                    'lines': 1000,
                    'estimated_time': 120,
                    'layer_count': 50
                }
            }
            
        except Exception as e:
            return {'success': False, 'error': str(e)}
    
    def get_slicer_version(self):
        try:
            result = subprocess.run(['prusa-slicer', '--version'], 
                                  capture_output=True, text=True, timeout=5)
            return result.stdout.strip() if result.returncode == 0 else 'unknown'
        except:
            return 'unknown'
    
    def get_available_memory(self):
        try:
            with open('/proc/meminfo', 'r') as f:
                for line in f:
                    if line.startswith('MemAvailable:'):
                        return int(line.split()[1]) * 1024
            return 0
        except:
            return 0
    
    def get_uptime(self):
        try:
            with open('/proc/uptime', 'r') as f:
                return float(f.read().split()[0])
        except:
            return 0

# Use minimal thread pool
class MinimalTCPServer(socketserver.ThreadingTCPServer):
    daemon_threads = True
    max_children = 2  # Limit concurrent connections

PORT = 9090
print(f'Starting MINIMAL HTTP server on port {PORT}...')

with MinimalTCPServer(('', PORT), MinimalSlicingHandler) as httpd:
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print('Shutting down server...')
        httpd.shutdown()
" &

# Wait for server to start
sleep 2

# Keep container running
wait