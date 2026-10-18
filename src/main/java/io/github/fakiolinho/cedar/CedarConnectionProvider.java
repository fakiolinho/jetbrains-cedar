package io.github.fakiolinho.cedar;

import com.intellij.openapi.diagnostic.Logger;
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

    public CedarConnectionProvider() {
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
            SchemaFinder.find(file, root).ifPresent(schema -> associate(languageServer, uri, schema));
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

    /** The server writes {@code ./logs/server.log} into its cwd; keep that out of the project. */
    private static @Nullable String scratchDir() {
        try {
            return Files.createTempDirectory("cedar-language-server").toString();
        } catch (IOException e) {
            return null;
        }
    }
}
