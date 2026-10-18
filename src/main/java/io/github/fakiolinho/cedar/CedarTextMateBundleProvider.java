package io.github.fakiolinho.cedar;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Serves the Cedar grammar (vendored from cedar-policy/vscode-cedar) to the bundled TextMate plugin.
 *  The TextMate plugin wants a directory on disk, so the bundle is unpacked from resources into the
 *  plugin's own folder, keyed by version so upgrades refresh it. */
public class CedarTextMateBundleProvider implements TextMateBundleProvider {

    static final String PLUGIN_ID = "io.github.fakiolinho.cedar";
    private static final List<String> FILES = List.of(
            "package.json", "language-configuration.json", "LICENSE", "NOTICE",
            "syntaxes/cedar.tmLanguage.json", "syntaxes/cedarschema.tmLanguage.json");

    @Override
    public @NotNull List<PluginBundle> getBundles() {
        PluginDescriptor plugin = Objects.requireNonNull(PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID)));
        Path target = plugin.getPluginPath().resolve("bundles").resolve(plugin.getVersion()).resolve("cedar");
        try {
            for (String name : FILES) {
                Path out = target.resolve(name);
                if (Files.notExists(out)) {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = Objects.requireNonNull(
                            getClass().getResourceAsStream("/textmate/cedar/" + name), name)) {
                        Files.copy(in, out);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.of(new PluginBundle("cedar", target));
    }
}
