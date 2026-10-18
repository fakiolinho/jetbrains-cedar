package io.github.fakiolinho.cedar;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class SchemaFinderTest {

    @Test
    public void nearestSchemaWinsAndRootIsTheCeiling() throws IOException {
        Path root = Files.createTempDirectory("cedar");
        Path outer = Files.createFile(root.resolve("outer.cedarschema"));
        Path pol = Files.createDirectories(root.resolve("a/b/policies"));
        Path inner = Files.createFile(root.resolve("a/inner.cedarschema.json"));
        Path policy = pol.resolve("p.cedar");

        assertEquals(inner, SchemaFinder.find(policy, root).orElseThrow());
        assertEquals(outer, SchemaFinder.find(root.resolve("x.cedar"), root).orElseThrow());
        // root stops the walk: a schema above it is never picked
        assertTrue(SchemaFinder.find(policy, pol).isEmpty());
        assertTrue(SchemaFinder.find(Path.of("/nonexistent/p.cedar"), null).isEmpty());
    }

    @Test
    public void onlyPoliciesAndEntitiesGetSchemas() {
        assertTrue(SchemaFinder.wantsSchema("file:///x/p.cedar"));
        assertTrue(SchemaFinder.wantsSchema("file:///x/e.cedarentities.json"));
        assertFalse(SchemaFinder.wantsSchema("file:///x/s.cedarschema"));
        assertFalse(SchemaFinder.wantsSchema("file:///x/s.cedarschema.json"));
    }
}
