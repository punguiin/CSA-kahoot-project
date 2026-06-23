package kahoot.protocol;

public enum MessageType {

    REQ_CREATE_ROOM(1),
    REQ_JOIN_ROOM(2),
    REQ_START_QUIZ(3),
    REQ_SUBMIT_ANSWER(4),
    REQ_NEXT_QUESTION(5),
    REQ_END_ROUND(6),
    REQ_GET_LEADERBOARD(7),

    ROOM_CREATED(20),
    JOIN_ACCEPTED(21),
    ANSWER_RESULT(22),
    ERROR(23),

    PLAYER_JOINED(40),
    QUESTION(41),
    LEADERBOARD(42),
    GAME_FINISHED(43),
    PLAYER_LEFT(44);

    private final int code;

    MessageType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public boolean isRequest() {
        return code >= 1 && code <= 19;
    }

    public static MessageType fromCode(int code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message code: " + code);
    }
}
