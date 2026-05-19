const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

/**
 * Fetches the initial snapshot of all FX rates.
 * @returns {Promise<Array>} A promise that resolves to the array of rates.
 */
export async function fetchRates() {
  const response = await fetch(`${API_BASE_URL}/api/rates`);
  if (!response.ok) {
    throw new Error(`Failed to fetch rates: ${response.statusText}`);
  }
  return response.json();
}
