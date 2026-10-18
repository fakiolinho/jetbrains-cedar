package io.github.fakiolinho.cedar;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.LanguageServerItem;
import com.redhat.devtools.lsp4ij.commands.LSPCommand;
import com.redhat.devtools.lsp4ij.commands.LSPCommandAction;
import org.jetbrains.annotations.NotNull;

/** Backs the server's "Click to associate schema" code lens ({@code cedar.schemaOptions}), which in
 *  VS Code is implemented by the extension, not the server. Opens a chooser and sends
 *  {@code cedar.associateSchema}. */
public class SchemaOptionsAction extends LSPCommandAction {

    /** Shape of the code lens argument: {@code {"document_uri": "file:///..."}}. */
    public static class Arg {
        public String document_uri;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    protected @NotNull ActionUpdateThread getCommandPerformedThread() {
        return ActionUpdateThread.EDT; // file chooser needs the EDT
    }

    @Override
    protected void commandPerformed(@NotNull LSPCommand command, @NotNull AnActionEvent e) {
        LanguageServerItem server = getLanguageServer(e);
        Arg arg = command.getArgumentAt(0, Arg.class);
        if (server == null || arg == null || arg.document_uri == null) {
            return;
        }
        FileChooserDescriptor d = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Cedar Schema")
                .withFileFilter(f -> f.getName().endsWith(".cedarschema") || f.getName().endsWith(".cedarschema.json"));
        VirtualFile chosen = FileChooser.chooseFile(d, e.getProject(), null);
        if (chosen != null) {
            CedarConnectionProvider.associate(server.getServer(), arg.document_uri, chosen.toNioPath());
        }
    }
}
