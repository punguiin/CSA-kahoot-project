import { WS_URL } from './config';
import { type DecodedPacket, MessageType, decodePacket, encodePacket } from './protocol';

type Handler = (payload: any, packet: DecodedPacket) => void;
type Role = 'HOST' | 'PLAYER';

interface Session {
    pin: string;
    nickname: string | null;
    role: Role;
}

const SESSION_KEY = 'kahoot.session';

class GameClient {
    private ws: WebSocket | null = null;
    private connecting: Promise<void> | null = null;
    private pktId = 1;
    private handlers = new Map<MessageType, Set<Handler>>();

    connId = 0;

    lastQuestion: any = null;

    session: Session | null = loadSession();

    isOpen(): boolean {
        return this.ws !== null && this.ws.readyState === WebSocket.OPEN;
    }

    setSession(pin: string, nickname: string | null, role: Role): void {
        this.session = { pin, nickname, role };
        try {
            sessionStorage.setItem(SESSION_KEY, JSON.stringify(this.session));
        } catch {

        }
    }

    clearSession(): void {
        this.session = null;
        this.lastQuestion = null;
        try {
            sessionStorage.removeItem(SESSION_KEY);
        } catch {

        }
    }

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

    async resume(): Promise<void> {
        const s = this.session;
        if (!s) {
            throw new Error('No session to resume');
        }
        await this.connect();
        const ackType = s.role === 'HOST' ? MessageType.ROOM_CREATED : MessageType.JOIN_ACCEPTED;
        const ack = this.once(ackType);
        this.send(MessageType.REQ_REJOIN, s.role === 'HOST' ? { pin: s.pin } : { pin: s.pin, nickname: s.nickname });
        await ack;
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

function loadSession(): Session | null {
    try {
        const raw = sessionStorage.getItem(SESSION_KEY);
        return raw ? (JSON.parse(raw) as Session) : null;
    } catch {
        return null;
    }
}

export const gameClient = new GameClient();
export { MessageType } from './protocol';
