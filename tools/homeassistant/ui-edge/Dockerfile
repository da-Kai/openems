FROM openems/ui-edge:2025.9.0
RUN apk add --no-cache jq bash
COPY run.sh /run.sh
RUN chmod +x /run.sh
ENTRYPOINT ["/run.sh"]