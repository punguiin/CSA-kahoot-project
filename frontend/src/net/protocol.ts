export const MessageType = {

    REQ_CREATE_ROOM: 1,
    REQ_JOIN_ROOM: 2,
    REQ_START_QUIZ: 3,
    REQ_SUBMIT_ANSWER: 4,
    REQ_NEXT_QUESTION: 5,
    REQ_END_ROUND: 6,
    REQ_GET_LEADERBOARD: 7,
    REQ_REJOIN: 8,

    ROOM_CREATED: 20,
    JOIN_ACCEPTED: 21,
    ANSWER_RESULT: 22,
    ERROR: 23,

    PLAYER_JOINED: 40,
    QUESTION: 41,
    LEADERBOARD: 42,
    GAME_FINISHED: 43,
    PLAYER_LEFT: 44,
    ROOM_CLOSED: 45,
} as const;

export type MessageType = typeof MessageType[keyof typeof MessageType];

export interface DecodedPacket {
    type: MessageType;
    connId: number;
    pktId: number;
    payload: any;
}

const MAGIC = 0x13;
const HEADER_LEN = 16;

const CRC_TABLE = buildCrcTable();

function buildCrcTable(): Uint16Array {

    const table = new Uint16Array(256);
    for (let i = 0; i < 256; i++) {
        let crc = i;
        for (let bit = 0; bit < 8; bit++) {
            crc = (crc & 1) !== 0 ? (crc >>> 1) ^ 0xa001 : crc >>> 1;
        }
        table[i] = crc;
    }
    return table;
}

function crc16(bytes: Uint8Array): number {
    let crc = 0x0000;
    for (let i = 0; i < bytes.length; i++) {
        crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ bytes[i]) & 0xff];
    }
    return crc & 0xffff;
}

export function encodePacket(type: MessageType, payload: unknown, pktId: number, src = 1, connId = 0): ArrayBuffer {
    const json = new TextEncoder().encode(JSON.stringify(payload ?? {}));
    const wLen = 8 + json.length;

    const frame = new Uint8Array(HEADER_LEN + wLen + 2);
    const dv = new DataView(frame.buffer);

    frame[0] = MAGIC;
    frame[1] = src & 0xff;
    dv.setBigInt64(2, BigInt(pktId), false);
    dv.setInt32(10, wLen, false);
    dv.setUint16(14, crc16(frame.subarray(0, 14)), false);

    dv.setInt32(HEADER_LEN, type, false);
    dv.setInt32(HEADER_LEN + 4, connId, false);
    frame.set(json, HEADER_LEN + 8);

    const message = frame.subarray(HEADER_LEN, HEADER_LEN + wLen);
    dv.setUint16(HEADER_LEN + wLen, crc16(message), false);

    return frame.buffer;
}

export function decodePacket(buffer: ArrayBuffer): DecodedPacket {
    const bytes = new Uint8Array(buffer);
    const dv = new DataView(buffer);

    if (bytes[0] !== MAGIC) {
        throw new Error(`Bad magic byte: ${bytes[0]}`);
    }
    const wLen = dv.getInt32(10, false);

    const headCrc = dv.getUint16(14, false);
    if (headCrc !== crc16(bytes.subarray(0, 14))) {
        throw new Error('Head CRC mismatch');
    }

    const message = bytes.subarray(HEADER_LEN, HEADER_LEN + wLen);
    const tailCrc = dv.getUint16(HEADER_LEN + wLen, false);
    if (tailCrc !== crc16(message)) {
        throw new Error('Tail CRC mismatch');
    }

    const type = dv.getInt32(HEADER_LEN, false) as MessageType;
    const connId = dv.getInt32(HEADER_LEN + 4, false);
    const jsonBytes = bytes.subarray(HEADER_LEN + 8, HEADER_LEN + wLen);
    const payload = jsonBytes.length > 0 ? JSON.parse(new TextDecoder().decode(jsonBytes)) : {};
    const pktId = Number(dv.getBigInt64(2, false));

    return { type, connId, pktId, payload };
}
