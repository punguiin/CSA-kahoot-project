// WebSocket gateway URL. Override with VITE_WS_URL in an .env file if the backend runs elsewhere.
export const WS_URL: string =
    (import.meta as any).env?.VITE_WS_URL || 'ws://localhost:9092';
