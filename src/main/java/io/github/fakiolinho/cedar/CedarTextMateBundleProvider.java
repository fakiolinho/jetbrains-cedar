package io.github.fakiolinho.cedar;

import com.intellij.openapi.application.PathManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/** Serves the Cedar grammar (vendored from cedar-policy/vscode-cedar) to the bundled TextMate plugin.
 *  The TextMate plugin wants a directory on disk, so the bundle is unpacked from resources into the
 *  IDE's system directory, overwritten on every load so upgrades refresh it. */
public class CedarTextMateBundleProvider implements TextMateBundleProvider {

    private static final List<String> FILES = List.of(
            "package.json", "language-configuration.json", "LICENSE", "NOTICE",
            "syntaxes/cedar.tmLanguage.json", "syntaxes/cedarschema.tmLanguage.json");

    @Override
    public @NotNull List<PluginBundle> getBundles() {
        Path target = PathManager.getSystemDir().resolve("cedar").resolve("textmate");
        try {
            for (String name : FILES) {
                Path out = target.resolve(name);
                Files.createDirectories(out.getParent());
                try (InputStream in = Objects.requireNonNull(
                        getClass().getResourceAsStream("/textmate/cedar/" + name), name)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.of(new PluginBundle("cedar", target));
    }
}
