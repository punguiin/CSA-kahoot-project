export const WS_URL: string =
    (import.meta as any).env?.VITE_WS_URL || 'ws://localhost:9092';

export const API_URL: string =
    (import.meta as any).env?.VITE_API_URL || 'http://localhost:8090';
