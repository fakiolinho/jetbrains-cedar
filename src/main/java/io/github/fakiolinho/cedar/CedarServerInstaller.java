package io.github.fakiolinho.cedar;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.redhat.devtools.lsp4ij.installation.LanguageServerInstallerBase;
import com.redhat.devtools.lsp4ij.installation.ServerInstallationContext;
import com.redhat.devtools.lsp4ij.installation.ServerInstallationStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Installs the server with cargo when it is missing. Upstream publishes no prebuilt binaries, so a Rust
 *  toolchain is the only route. Failures become a notification, not an exception: LSP4IJ keeps a failed install
 *  future and rethrows it from every editor feature (inlay hints, code vision...), flooding the IDE error log. */
public class CedarServerInstaller extends LanguageServerInstallerBase {

    private static final Logger LOG = Logger.getInstance(CedarServerInstaller.class);

    /** Thrown for failures already reported to the user, so {@link #checkInstallation} can swallow them. */
    private static final class ReportedFailure extends Exception {
        ReportedFailure(String message) {
            super(message, null, false, false);
        }
    }

    @Override
    public @NotNull CompletableFuture<ServerInstallationStatus> checkInstallation(@NotNull ServerInstallationContext context) {
        return super.checkInstallation(context).exceptionally(e -> {
            Throwable cause = e instanceof CompletionException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof CancellationException) {
                throw (CancellationException) cause;
            }
            if (!(cause instanceof ReportedFailure)) {
                LOG.warn("cedar-language-server installation failed", cause);
            }
            return ServerInstallationStatus.NOT_INSTALLED;
        });
    }

    @Override
    protected boolean checkServerInstalled(@NotNull ProgressIndicator indicator) {
        return Binaries.find(Binaries.SERVER).isPresent();
    }

    @Override
    protected void install(@NotNull ProgressIndicator indicator) throws Exception {
        Path cargo = Binaries.find("cargo").orElse(null);
        if (cargo == null) {
            throw fail("cargo was not found, so cedar-language-server can't be installed. Install Rust, then run "
                    + "\"Install\" again from the Cedar Language Server entry in the Language Servers tool window.");
        }
        indicator.setText2("cargo install cedar-language-server (builds from source, a few minutes)");
        GeneralCommandLine cmd = new GeneralCommandLine(cargo.toString(), "install",
                "--git", "https://github.com/cedar-policy/cedar", "cedar-language-server", "--locked");
        ProcessOutput out = new CapturingProcessHandler(cmd).runProcessWithProgressIndicator(indicator);
        if (out.isCancelled()) {
            throw new CancellationException();
        }
        if (out.getExitCode() != 0) {
            LOG.warn("cargo install cedar-language-server failed:\n" + out.getStderr());
            throw fail("cargo install cedar-language-server failed (exit code " + out.getExitCode()
                    + "). See idea.log for cargo's output.");
        }
    }

    private ReportedFailure fail(String message) {
        var notification = NotificationGroupManager.getInstance().getNotificationGroup("Cedar")
                .createNotification("Cedar language server not installed", message, NotificationType.WARNING);
        if (message.startsWith("cargo was not found")) {
            notification.addAction(NotificationAction.createSimple("Open rustup.rs",
                    () -> BrowserUtil.browse("https://rustup.rs")));
        }
        notification.notify(getProject());
        return new ReportedFailure(message);
    }
}
