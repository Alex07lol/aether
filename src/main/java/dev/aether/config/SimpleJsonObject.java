package dev.aether.config;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class SimpleJsonObject {
    private final Map<String, String> strings = new LinkedHashMap<String, String>();
    private final Map<String, SimpleJsonObject> objects = new LinkedHashMap<String, SimpleJsonObject>();

    static SimpleJsonObject parse(String json) throws IOException {
        return new Parser(json).parseObject();
    }

    void putString(String key, String value) {
        strings.put(key, value == null ? "" : value);
    }

    void putObject(String key, SimpleJsonObject value) {
        objects.put(key, value);
    }

    String getString(String key) {
        return strings.get(key);
    }

    SimpleJsonObject getObject(String key) throws IOException {
        SimpleJsonObject object = objects.get(key);
        if (object == null) {
            throw new IOException("Missing JSON object: " + key);
        }
        return object;
    }

    Map<String, String> strings() {
        return Collections.unmodifiableMap(strings);
    }

    String toPrettyJson() {
        StringBuilder out = new StringBuilder();
        writeObject(out, 0);
        out.append('\n');
        return out.toString();
    }

    private void writeObject(StringBuilder out, int indent) {
        out.append("{\n");
        int index = 0;
        int size = strings.size() + objects.size();
        for (Map.Entry<String, String> entry : strings.entrySet()) {
            writeIndent(out, indent + 2);
            out.append('"').append(escape(entry.getKey())).append("\": ");
            out.append('"').append(escape(entry.getValue())).append('"');
            if (++index < size) {
                out.append(',');
            }
            out.append('\n');
        }
        for (Map.Entry<String, SimpleJsonObject> entry : objects.entrySet()) {
            writeIndent(out, indent + 2);
            out.append('"').append(escape(entry.getKey())).append("\": ");
            entry.getValue().writeObject(out, indent + 2);
            if (++index < size) {
                out.append(',');
            }
            out.append('\n');
        }
        writeIndent(out, indent);
        out.append('}');
    }

    private static void writeIndent(StringBuilder out, int indent) {
        for (int i = 0; i < indent; i++) {
            out.append(' ');
        }
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static final class Parser {
        private final String input;
        private int offset;

        Parser(String input) {
            this.input = input == null ? "" : input;
        }

        SimpleJsonObject parseObject() throws IOException {
            skipWhitespace();
            expect('{');
            SimpleJsonObject object = new SimpleJsonObject();
            skipWhitespace();
            if (peek('}')) {
                offset++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                if (peek('{')) {
                    object.putObject(key, parseObject());
                } else {
                    object.putString(key, parseString());
                }
                skipWhitespace();
                if (peek(',')) {
                    offset++;
                    continue;
                }
                expect('}');
                return object;
            }
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (offset < input.length()) {
                char c = input.charAt(offset++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (offset >= input.length()) {
                        throw error("Unterminated escape sequence.");
                    }
                    char escaped = input.charAt(offset++);
                    if (escaped == '"' || escaped == '\\' || escaped == '/') {
                        out.append(escaped);
                    } else if (escaped == 'n') {
                        out.append('\n');
                    } else if (escaped == 'r') {
                        out.append('\r');
                    } else if (escaped == 't') {
                        out.append('\t');
                    } else {
                        throw error("Unsupported escape sequence: \\" + escaped);
                    }
                } else {
                    out.append(c);
                }
            }
            throw error("Unterminated string.");
        }

        private void skipWhitespace() {
            while (offset < input.length()) {
                char c = input.charAt(offset);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    offset++;
                } else {
                    return;
                }
            }
        }

        private boolean peek(char expected) {
            return offset < input.length() && input.charAt(offset) == expected;
        }

        private void expect(char expected) throws IOException {
            if (!peek(expected)) {
                throw error("Expected '" + expected + "'.");
            }
            offset++;
        }

        private IOException error(String message) {
            return new IOException(message + " Offset " + offset + ".");
        }
    }
}

