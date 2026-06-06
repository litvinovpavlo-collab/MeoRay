package dev.meoray.client.managers;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class FriendManager {
    private final Set<String> friends = new LinkedHashSet<>();

    public boolean isFriend(String name) {
        if (name == null) return false;
        return friends.contains(name.toLowerCase());
    }

    public void add(String name) {
        if (name == null || name.isEmpty()) return;
        friends.add(name.toLowerCase());
    }

    public void remove(String name) {
        if (name == null) return;
        friends.remove(name.toLowerCase());
    }

    public boolean toggle(String name) {
        if (name == null || name.isEmpty()) return false;
        String key = name.toLowerCase();
        if (friends.contains(key)) {
            friends.remove(key);
            return false;
        } else {
            friends.add(key);
            return true;
        }
    }

    public Set<String> getAll() {
        return Collections.unmodifiableSet(friends);
    }

    public void clear() {
        friends.clear();
    }
}
