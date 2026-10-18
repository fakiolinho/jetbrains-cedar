package io.github.fakiolinho.cedar;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.server.CannotStartProcessException;
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.jsonrpc.messages.NotificationMessage;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Launches {@code cedar-language-server} over stdio and, on every {@code textDocument/didOpen} of a policy
 *  or entities file, tells the server which schema to validate it against (the server has no discovery of
 *  its own; VS Code does this client side and so do we). */
public class CedarConnectionProvider extends ProcessStreamConnectionProvider {

    private static final Logger LOG = Logger.getInstance(CedarConnectionProvider.class);
    private static final String MUTE_KEY = "cedar.noSchemaNotification.muted";
    private static final String HELP_URL = "https://github.com/fakiolinho/jetbrains-cedar#schema-association";

    private final Project project;
    /** Schema picked from the "no schema" notification; used for later files that have none nearby. */
    private volatile @Nullable Path chosenSchema;
    private volatile boolean notified;

    public CedarConnectionProvider(@NotNull Project project) {
        this.project = project;
        Binaries.find(Binaries.SERVER).ifPresent(p -> setCommands(List.of(p.toString())));
        setWorkingDirectory(scratchDir());
    }

    @Override
    public void start() throws CannotStartProcessException {
        if (getCommands() == null || getCommands().isEmpty()) {
            throw new CannotStartProcessException(
                    "cedar-language-server not found on PATH or in ~/.cargo/bin. Install with:\n"
                    + "cargo install --git https://github.com/cedar-policy/cedar cedar-language-server --locked");
        }
        super.start();
    }

    @Override
    public void handleMessage(Message message, LanguageServer languageServer, VirtualFile rootUri) {
        if (!(message instanceof NotificationMessage n) || !"textDocument/didOpen".equals(n.getMethod())
                || !(n.getParams() instanceof DidOpenTextDocumentParams p)) {
            return;
        }
        String uri = p.getTextDocument().getUri();
        if (!SchemaFinder.wantsSchema(uri)) {
            return;
        }
        try {
            Path file = Path.of(URI.create(uri));
            Path root = rootUri == null ? null : rootUri.toNioPath();
            Path schema = SchemaFinder.find(file, root).orElse(chosenSchema);
            if (schema != null) {
                associate(languageServer, uri, schema);
            } else {
                notifyNoSchema(languageServer, uri, file.getFileName().toString());
            }
        } catch (RuntimeException e) {
            // non file:// uri (jar, scratch) or exotic VFS: nothing on disk to look around. Never let this
            // escape, it runs inside the LSP message pipeline.
            LOG.debug("no schema lookup for " + uri, e);
        }
    }

    static void associate(@NotNull LanguageServer server, @NotNull String documentUri, @Nullable Path schema) {
        Map<String, String> arg = schema == null
                ? Map.of("document_uri", documentUri)
                : Map.of("document_uri", documentUri, "schema_uri", schema.toUri().toString());
        server.getWorkspaceService()
                .executeCommand(new ExecuteCommandParams("cedar.associateSchema", List.of(arg)))
                .exceptionally(e -> {
                    LOG.warn("cedar.associateSchema failed for " + documentUri, e);
                    return null;
                });
    }

    /** Without a schema the server only checks syntax, and its "Click to associate schema" code lens doesn't say
     *  why. Explain once per project and session, offering to pick a schema for this and later files. */
    private void notifyNoSchema(@NotNull LanguageServer server, @NotNull String uri, @NotNull String fileName) {
        PropertiesComponent props = PropertiesComponent.getInstance(project);
        if (notified || props.getBoolean(MUTE_KEY)) {
            return;
        }
        notified = true;
        NotificationGroupManager.getInstance().getNotificationGroup("Cedar")
                .createNotification("No Cedar schema found",
                        fileName + " is only checked for syntax. Add a <code>*.cedarschema</code> file next to your "
                        + "policies or in a parent folder, and it is used automatically.",
                        NotificationType.INFORMATION)
                .addAction(NotificationAction.createSimpleExpiring("Choose schema…", () -> {
                    VirtualFile chosen = SchemaOptionsAction.chooseSchema(project);
                    if (chosen != null) {
                        chosenSchema = chosen.toNioPath();
                        associate(server, uri, chosenSchema);
                    }
                }))
                .addAction(NotificationAction.createSimple("How schemas are found", () -> BrowserUtil.browse(HELP_URL)))
                .addAction(new NotificationAction("Don't show again") {
                    @Override
                    public void actionPerformed(@NotNull com.intellij.openapi.actionSystem.AnActionEvent e,
                                                @NotNull Notification n) {
                        props.setValue(MUTE_KEY, true);
                        n.expire();
                    }
                })
                .notify(project);
    }

    /** The server writes {@code ./logs/server.log} into its cwd; keep that out of the project. */
    private static @Nullable String scratchDir() {
        try {
            return Files.createTempDirectory("cedar-language-server").toString();
        } catch (IOException e) {
            return null;
        }
    }
}
