package io.github.fakiolinho.cedar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/** Locates the Cedar schema that applies to a policy or entities file: nearest {@code *.cedarschema}
 *  (or {@code *.cedarschema.json}) walking up from the file's directory, never above {@code root}. */
public final class SchemaFinder {

    private SchemaFinder() {}

    public static boolean wantsSchema(String uri) {
        return uri.endsWith(".cedar") || uri.endsWith(".cedarentities.json");
    }

    public static Optional<Path> find(Path file, Path root) {
        for (Path dir = file.getParent(); dir != null; dir = dir.getParent()) {
            Optional<Path> hit = firstSchemaIn(dir);
            if (hit.isPresent() || (root != null && dir.equals(root))) {
                return hit;
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> firstSchemaIn(Path dir) {
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (Stream<Path> s = Files.list(dir)) {
            // ponytail: first match in name order; multiple schemas in one dir are rare and the
            // code lens lets the user pick explicitly.
            return s.filter(p -> {
                        String n = p.getFileName().toString();
                        return n.endsWith(".cedarschema") || n.endsWith(".cedarschema.json");
                    })
                    .sorted()
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
