package io.github.fakiolinho.cedar;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.IconUtil;

import javax.swing.Icon;

/** plugin.xml maps the Cedar extensions onto TextMate's file type; a typo there would silently
 *  drop highlighting (files fall back to plain text). */
public class CedarFileTypeTest extends BasePlatformTestCase {

    public void testCedarFilesUseTextMate() {
        FileTypeManager ftm = FileTypeManager.getInstance();
        assertEquals("textmate", ftm.getFileTypeByFileName("policy.cedar").getName());
        assertEquals("textmate", ftm.getFileTypeByFileName("agentcore.cedarschema").getName());
        assertEquals("textmate", ftm.getFileTypeByFileName("cedarschema").getName());
    }

    public void testCedarFilesGetVscodeCedarIcons() {
        assertSame(CedarIconProvider.POLICY, iconOf("policy.cedar"));
        assertSame(CedarIconProvider.SCHEMA, iconOf("agentcore.cedarschema"));
        assertSame(CedarIconProvider.SCHEMA, iconOf("cedarschema"));
        assertNotSame(CedarIconProvider.SCHEMA, iconOf("agentcore.cedarschema.json"));
    }

    private Icon iconOf(String name) {
        return IconUtil.getIcon(myFixture.getTempDirFixture().createFile(name), 0, getProject());
    }
}
