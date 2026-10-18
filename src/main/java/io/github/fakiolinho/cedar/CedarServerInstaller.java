package io.github.fakiolinho.cedar;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.progress.ProgressIndicator;
import com.redhat.devtools.lsp4ij.installation.LanguageServerInstallerBase;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/** Installs the server with cargo when it is missing. Upstream publishes no prebuilt binaries, so a Rust
 *  toolchain is the only route; a clear error beats a silent failure when cargo is absent. */
public class CedarServerInstaller extends LanguageServerInstallerBase {

    @Override
    protected boolean checkServerInstalled(@NotNull ProgressIndicator indicator) {
        return Binaries.find(Binaries.SERVER).isPresent();
    }

    @Override
    protected void install(@NotNull ProgressIndicator indicator) throws Exception {
        Path cargo = Binaries.find("cargo").orElseThrow(() -> new IllegalStateException(
                "cargo not found. Install Rust from https://rustup.rs, then rerun \"Install\" for the Cedar server."));
        indicator.setText2("cargo install cedar-language-server (builds from source, a few minutes)");
        GeneralCommandLine cmd = new GeneralCommandLine(cargo.toString(), "install",
                "--git", "https://github.com/cedar-policy/cedar", "cedar-language-server", "--locked");
        ProcessOutput out = new CapturingProcessHandler(cmd).runProcessWithProgressIndicator(indicator);
        if (out.getExitCode() != 0) {
            throw new IllegalStateException("cargo install failed:\n" + out.getStderr());
        }
    }
}
