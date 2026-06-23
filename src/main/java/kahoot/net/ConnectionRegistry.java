package kahoot.net;

import kahoot.protocol.Packet;
import kahoot.transport.OutboundSink;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionRegistry {

    public enum Role { HOST, PLAYER }

    public record Identity(String pin, String nickname, Role role) {
    }

    private final ConcurrentHashMap<Integer, OutboundSink> sinks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Integer>> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Identity> identities = new ConcurrentHashMap<>();

    public void register(OutboundSink sink) {
        sinks.put(sink.connId(), sink);
    }

    public void bindHost(int connId, String pin) {
        identities.put(connId, new Identity(pin, null, Role.HOST));
        rooms.computeIfAbsent(pin, k -> ConcurrentHashMap.newKeySet()).add(connId);
    }

    public void bindPlayer(int connId, String pin, String nickname) {
        identities.put(connId, new Identity(pin, nickname, Role.PLAYER));
        rooms.computeIfAbsent(pin, k -> ConcurrentHashMap.newKeySet()).add(connId);
    }

    public Identity identityOf(int connId) {
        return identities.get(connId);
    }

    public void sendTo(int connId, Packet packet) {
        OutboundSink sink = sinks.get(connId);
        if (sink != null) {
            sink.send(packet);
        }
    }

    public void broadcast(String pin, Packet packet, int excludeConnId) {
        Set<Integer> members = rooms.get(pin);
        if (members == null) {
            return;
        }
        for (Integer id : members) {
            if (id == excludeConnId) {
                continue;
            }
            sendTo(id, packet);
        }
    }

    public Identity unregister(int connId) {
        sinks.remove(connId);
        Identity identity = identities.remove(connId);
        if (identity != null) {
            Set<Integer> members = rooms.get(identity.pin());
            if (members != null) {
                members.remove(connId);
                if (members.isEmpty()) {
                    rooms.remove(identity.pin());
                }
            }
        }
        return identity;
    }
}
