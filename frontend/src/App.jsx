import { useEffect, useState } from "react";
import { fetchRates } from "./api/ratesApi";
import { createRatesSocket } from "./ws/ratesSocket";
import "./App.css";

function App() {
  const [rates, setRates] = useState({});
  const [status, setStatus] = useState("Connecting");
  const [flashPairs, setFlashPairs] = useState({});
  const [error, setError] = useState(null);

  useEffect(() => {
    // 1. Fetch initial snapshot
    fetchRates()
      .then((data) => {
        const initialRates = {};
        data.forEach((rate) => {
          initialRates[rate.pair] = rate;
        });
        setRates(initialRates);
      })
      .catch((err) => {
        console.error("Failed to load initial snapshot:", err);
        setError("Could not load initial rate snapshot.");
      });

    // 2. Open WebSocket connection
    const ws = createRatesSocket(
      (message) => {
        if (message && message.type === "RATE_UPDATE" && message.data) {
          const rateData = message.data;
          setRates((prev) => ({
            ...prev,
            [rateData.pair]: rateData,
          }));

          // Trigger subtle row flash highlight
          setFlashPairs((prev) => ({
            ...prev,
            [rateData.pair]: true,
          }));
          setTimeout(() => {
            setFlashPairs((prev) => ({
              ...prev,
              [rateData.pair]: false,
            }));
          }, 800);
        }
      },
      (newStatus) => {
        setStatus(newStatus);
      }
    );

    // Clean up on component unmount
    return () => {
      ws.disconnect();
    };
  }, []);

  const ratesList = Object.values(rates).sort((a, b) =>
    a.pair.localeCompare(b.pair)
  );

  const getStatusClass = () => {
    switch (status) {
      case "Connected":
        return "status-connected";
      case "Connecting":
        return "status-connecting";
      default:
        return "status-disconnected";
    }
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="title-section">
          <h1>FX Rate Hub</h1>
          <p className="subtitle">Real-Time Exchange Rate Dashboard</p>
        </div>
        <div className={`status-indicator ${getStatusClass()}`}>
          <span className="status-dot"></span>
          <span className="status-text">{status}</span>
        </div>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <main className="app-main">
        <div className="table-card">
          <table className="rates-table">
            <thead>
              <tr>
                <th>Currency Pair</th>
                <th>Provider</th>
                <th>Bid</th>
                <th>Ask</th>
                <th>Spread</th>
                <th>Alarm</th>
                <th>Timestamp</th>
                <th>Received At</th>
              </tr>
            </thead>
            <tbody>
              {ratesList.length === 0 ? (
                <tr>
                  <td colSpan="8" className="empty-row">
                    No active rates. Waiting for updates...
                  </td>
                </tr>
              ) : (
                ratesList.map((rate) => {
                  const isAlarmActive = rate.alarm;
                  const isFlashing = flashPairs[rate.pair];
                  
                  return (
                    <tr
                      key={rate.pair}
                      className={`${isFlashing ? "flash-row" : ""} ${
                        isAlarmActive ? "alarm-active-row" : ""
                      }`}
                    >
                      <td className="bold-text">{rate.pair}</td>
                      <td>
                        <span className="provider-badge">{rate.provider}</span>
                      </td>
                      <td className="price-text">{Number(rate.bid).toFixed(4)}</td>
                      <td className="price-text">{Number(rate.ask).toFixed(4)}</td>
                      <td className="price-text spread-text">
                        {Number(rate.spread).toFixed(4)}
                      </td>
                      <td>
                        {isAlarmActive ? (
                          <span className="alarm-badge">ALARM</span>
                        ) : (
                          <span className="alarm-normal">—</span>
                        )}
                      </td>
                      <td className="timestamp-text">
                        {new Date(rate.timestamp).toLocaleTimeString()}
                      </td>
                      <td className="timestamp-text">
                        {new Date(rate.receivedAt).toLocaleTimeString()}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  );
}

export default App;
