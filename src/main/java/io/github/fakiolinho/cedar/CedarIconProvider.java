package io.github.fakiolinho.cedar;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/** Cedar files belong to TextMate's file type (see plugin.xml), which would give them its generic icon.
 *  The icons are vscode-cedar's ({@code icons/Cedar_16.svg}, {@code icons/CedarSchema_16.svg}), mapped the same way. */
public class CedarIconProvider implements FileIconProvider {

    static final Icon POLICY = IconLoader.getIcon("/icons/cedar.svg", CedarIconProvider.class);
    static final Icon SCHEMA = IconLoader.getIcon("/icons/cedarschema.svg", CedarIconProvider.class);

    @Override
    public @Nullable Icon getIcon(@NotNull VirtualFile file, int flags, @Nullable Project project) {
        String name = file.getName();
        if (name.endsWith(".cedar")) {
            return POLICY;
        }
        if (name.endsWith(".cedarschema") || name.equals("cedarschema")) {
            return SCHEMA;
        }
        return null;
    }
}
