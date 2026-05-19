const WS_URL = import.meta.env.VITE_WS_URL || "ws://localhost:8080/ws/rates";

/**
 * Creates and manages a WebSocket connection to the FX rates stream.
 * 
 * @param {Function} onMessage Callback invoked when a message is received.
 * @param {Function} onStatusChange Callback invoked when the connection status changes.
 * @returns {Object} An object with a disconnect method to clean up the socket.
 */
export function createRatesSocket(onMessage, onStatusChange) {
  let socket = null;
  let isClosedIntentional = false;
  let reconnectTimeout = null;

  function connect() {
    isClosedIntentional = false;
    onStatusChange("Connecting");

    socket = new WebSocket(WS_URL);

    socket.onopen = () => {
      onStatusChange("Connected");
      // Send subscription message for Step 8 required pairs
      const subscribeMsg = {
        type: "SUBSCRIBE",
        pairs: ["EUR/USD", "USD/TRY", "GBP/USD"]
      };
      socket.send(JSON.stringify(subscribeMsg));
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        onMessage(message);
      } catch (err) {
        console.error("Failed to parse WebSocket message:", err);
      }
    };

    socket.onclose = () => {
      onStatusChange("Disconnected");
      if (!isClosedIntentional) {
        // Attempt to reconnect after a short delay
        reconnectTimeout = setTimeout(connect, 3000);
      }
    };

    socket.onerror = (error) => {
      console.error("WebSocket error observed:", error);
      socket.close();
    };
  }

  connect();

  return {
    disconnect: () => {
      isClosedIntentional = true;
      if (reconnectTimeout) {
        clearTimeout(reconnectTimeout);
      }
      if (socket) {
        socket.close();
      }
    }
  };
}
