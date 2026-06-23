package kahoot.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {

    private Json() {
    }

    public static class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }

    public static final class Writer {
        private final StringBuilder sb = new StringBuilder();
        private boolean first = true;

        private Writer() {
            sb.append('{');
        }

        public static Writer object() {
            return new Writer();
        }

        public Writer str(String key, String value) {
            comma().key(key);
            if (value == null) {
                sb.append("null");
            } else {
                writeString(sb, value);
            }
            return this;
        }

        public Writer num(String key, long value) {
            comma().key(key).append(Long.toString(value));
            return this;
        }

        public Writer bool(String key, boolean value) {
            comma().key(key).append(value ? "true" : "false");
            return this;
        }

        public Writer raw(String key, String jsonFragment) {
            comma().key(key).append(jsonFragment);
            return this;
        }

        public String end() {
            sb.append('}');
            return sb.toString();
        }

        private Writer comma() {
            if (!first) {
                sb.append(',');
            }
            first = false;
            return this;
        }

        private StringBuilder key(String key) {
            writeString(sb, key);
            sb.append(':');
            return sb;
        }
    }

    public static String array(List<String> elements) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(elements.get(i));
        }
        return sb.append(']').toString();
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new JsonException("Expected a JSON object");
        }
        return (Map<String, Object>) v;
    }

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWs();
        Object value = p.readValue();
        p.skipWs();
        if (!p.atEnd()) {
            throw new JsonException("Trailing characters after JSON value at " + p.pos);
        }
        return value;
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        void skipWs() {
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        Object readValue() {
            if (atEnd()) {
                throw new JsonException("Unexpected end of input");
            }
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't', 'f' -> readBoolean();
                case 'n' -> readNull();
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWs();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWs();
                String key = readString();
                skipWs();
                expect(':');
                skipWs();
                map.put(key, readValue());
                skipWs();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new JsonException("Expected ',' or '}' in object at " + (pos - 1));
                }
            }
        }

        private List<Object> readArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWs();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWs();
                list.add(readValue());
                skipWs();
                char c = next();
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new JsonException("Expected ',' or ']' in array at " + (pos - 1));
                }
            }
        }

        private String readString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new JsonException("Unterminated string");
                }
                char c = s.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (pos + 4 > s.length()) {
                                throw new JsonException("Bad unicode escape");
                            }
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> throw new JsonException("Bad escape '\\" + e + "'");
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Boolean readBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonException("Invalid literal at " + pos);
        }

        private Object readNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonException("Invalid literal at " + pos);
        }

        private Long readNumber() {
            int start = pos;
            if (peek() == '-') {
                pos++;
            }
            while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
                pos++;
            }
            if (pos == start) {
                throw new JsonException("Invalid value at " + pos);
            }
            return Long.parseLong(s.substring(start, pos));
        }

        private char peek() {
            if (atEnd()) {
                throw new JsonException("Unexpected end of input");
            }
            return s.charAt(pos);
        }

        private char next() {
            if (atEnd()) {
                throw new JsonException("Unexpected end of input");
            }
            return s.charAt(pos++);
        }

        private void expect(char c) {
            char got = next();
            if (got != c) {
                throw new JsonException("Expected '" + c + "' but got '" + got + "' at " + (pos - 1));
            }
        }
    }
}
