# Dockerfile for PrusaSlicer Slicing Container
FROM ubuntu:22.04

LABEL maintainer="Andrea Redegalli <redegalli03@gmail.com>"
LABEL description="PrusaSlicer container for 3D model slicing"

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/Rome

# Install only ESSENTIAL runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Minimali per PrusaSlicer
    libgl1-mesa-glx \
    libglib2.0-0 \
    libgtk-3-0 \
    libdbus-glib-1-2 \
    # Minimali per funzionalità container
    curl \
    python3 \
    xvfb \
    # Pulizia aggressiva
    && apt-get autoremove -y \
    && apt-get autoclean \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /var/cache/apt/* \
    && rm -rf /usr/share/doc/* \
    && rm -rf /usr/share/man/* \
    && rm -rf /usr/share/locale/* \
    && rm -rf /tmp/* \
    && rm -rf /var/tmp/*

# Create minimal user
RUN useradd -m -s /bin/bash -u 1000 slicer && \
    mkdir -p /app/temp /app/output && \
    chown -R slicer:slicer /app

# Copy PrusaSlicer from builder stage
COPY --from=builder /tmp/squashfs-root /opt/prusaslicer
RUN ln -s /opt/prusaslicer/AppRun /usr/local/bin/prusa-slicer

# Copy ottimized scripts
COPY docker/optimized-entrypoint.sh /app/slicer-entrypoint.sh
COPY docker/minimal-health-check.sh /app/health-check.sh

RUN chmod +x /app/slicer-entrypoint.sh /app/health-check.sh && \
    chown slicer:slicer /app/slicer-entrypoint.sh /app/health-check.sh

# Switch to non-root user
USER slicer
WORKDIR /app

# Environment variables per ottimizzazione
ENV DISPLAY=:99
ENV SLICER_TEMP_DIR=/app/temp
ENV SLICER_OUTPUT_DIR=/app/output
ENV CONTAINER_ID=""
ENV JAVA_APP_HOST="host.docker.internal"
ENV JAVA_APP_PORT=8443

# Performance environment variables
ENV LOW_RESOURCE_MODE=true
ENV MEMORY_LIMIT_MB=512
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Expose port
EXPOSE 9090

# Ottimized health check - meno frequente
HEALTHCHECK --interval=60s --timeout=15s --start-period=30s --retries=2 \
    CMD /app/health-check.sh

# Default command
CMD ["/app/slicer-entrypoint.sh"]
