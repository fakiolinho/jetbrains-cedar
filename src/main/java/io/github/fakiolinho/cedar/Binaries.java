package io.github.fakiolinho.cedar;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Finds executables on the user's shell PATH (IDEs launched from the Dock/Finder get a bare PATH,
 *  so {@code ~/.cargo/bin} is checked explicitly). */
final class Binaries {

    static final String SERVER = "cedar-language-server";

    private Binaries() {}

    static Optional<Path> find(String name) {
        String exe = SystemInfo.isWindows ? name + ".exe" : name;
        String path = EnvironmentUtil.getValue("PATH");
        if (path != null) {
            for (String dir : path.split(java.io.File.pathSeparator)) {
                Path p = Path.of(dir, exe);
                if (Files.isExecutable(p)) {
                    return Optional.of(p);
                }
            }
        }
        Path cargoBin = Path.of(System.getProperty("user.home"), ".cargo", "bin", exe);
        return Files.isExecutable(cargoBin) ? Optional.of(cargoBin) : Optional.empty();
    }
}
