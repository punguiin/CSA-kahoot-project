import { WS_URL } from './config';
import { type DecodedPacket, MessageType, decodePacket, encodePacket } from './protocol';

type Handler = (payload: any, packet: DecodedPacket) => void;

// A module-level singleton so the WebSocket survives React route changes
// (host creates a room, then navigates to /host/:pin on the same connection).
class GameClient {
    private ws: WebSocket | null = null;
    private connecting: Promise<void> | null = null;
    private pktId = 1;
    private handlers = new Map<MessageType, Set<Handler>>();

    connId = 0;
    /** Last QUESTION payload, stashed so a freshly-navigated page can render immediately. */
    lastQuestion: any = null;

    connect(): Promise<void> {
        if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
            return this.connecting ?? Promise.resolve();
        }
        this.connecting = new Promise((resolve, reject) => {
            const ws = new WebSocket(WS_URL);
            ws.binaryType = 'arraybuffer';
            ws.onopen = () => resolve();
            ws.onerror = () => reject(new Error('WebSocket connection failed'));
            ws.onmessage = (ev) => this.dispatch(ev.data as ArrayBuffer);
            ws.onclose = () => {
                this.ws = null;
                this.connecting = null;
            };
            this.ws = ws;
        });
        return this.connecting;
    }

    send(type: MessageType, payload: Record<string, unknown> = {}): void {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.error('GameClient: cannot send, socket not open');
            return;
        }
        this.ws.send(encodePacket(type, payload, this.pktId++));
    }

    on(type: MessageType, handler: Handler): () => void {
        let set = this.handlers.get(type);
        if (!set) {
            set = new Set();
            this.handlers.set(type, set);
        }
        set.add(handler);
        return () => set!.delete(handler);
    }

    /** Resolve on the next packet of `type` (or reject on ERROR / timeout). */
    once(type: MessageType, timeoutMs = 5000): Promise<DecodedPacket> {
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                offType();
                offErr();
                reject(new Error(`Timed out waiting for message type ${type}`));
            }, timeoutMs);
            const offType = this.on(type, (_p, pkt) => {
                clearTimeout(timer);
                offType();
                offErr();
                resolve(pkt);
            });
            const offErr = this.on(MessageType.ERROR, (payload) => {
                clearTimeout(timer);
                offType();
                offErr();
                reject(new Error(payload?.message || 'Server error'));
            });
        });
    }

    disconnect(): void {
        this.ws?.close();
        this.ws = null;
        this.connecting = null;
        this.handlers.clear();
        this.lastQuestion = null;
    }

    private dispatch(buffer: ArrayBuffer): void {
        let pkt: DecodedPacket;
        try {
            pkt = decodePacket(buffer);
        } catch (e) {
            console.error('GameClient: failed to decode packet', e);
            return;
        }
        if (pkt.connId !== 0) {
            this.connId = pkt.connId;
        }
        if (pkt.type === MessageType.QUESTION) {
            this.lastQuestion = pkt.payload;
        }
        this.handlers.get(pkt.type)?.forEach((h) => h(pkt.payload, pkt));
    }
}

export const gameClient = new GameClient();
export { MessageType } from './protocol';
