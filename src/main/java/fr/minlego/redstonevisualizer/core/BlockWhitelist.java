package fr.minlego.redstonevisualizer.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** A small whitelist of validated namespaced block identifiers. */
public final class BlockWhitelist {
    private static final Pattern IDENTIFIER = Pattern.compile(
            "[a-z0-9_.-]+:[a-z0-9/._-]+");

    private final Set<String> identifiers = new LinkedHashSet<>();

    public BlockWhitelist() {
    }

    public BlockWhitelist(Collection<String> identifiers) {
        Objects.requireNonNull(identifiers, "identifiers");
        for (String identifier : identifiers) {
            add(identifier);
        }
    }

    public static boolean isValidIdentifier(String identifier) {
        return identifier != null && IDENTIFIER.matcher(identifier).matches();
    }

    public boolean add(String identifier) {
        requireValid(identifier);
        return identifiers.add(identifier);
    }

    public boolean addIfValid(String identifier) {
        return isValidIdentifier(identifier) && identifiers.add(identifier);
    }

    public boolean remove(String identifier) {
        return identifiers.remove(identifier);
    }

    public boolean contains(String identifier) {
        return identifiers.contains(identifier);
    }

    public void clear() {
        identifiers.clear();
    }

    public int size() {
        return identifiers.size();
    }

    public Set<String> identifiers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(identifiers));
    }

    private static void requireValid(String identifier) {
        if (!isValidIdentifier(identifier)) {
            throw new IllegalArgumentException("invalid block identifier: " + identifier);
        }
    }
}
