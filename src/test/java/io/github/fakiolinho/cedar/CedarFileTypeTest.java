package io.github.fakiolinho.cedar;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/** plugin.xml maps the Cedar extensions onto TextMate's file type; a typo there would silently
 *  drop highlighting (files fall back to plain text). */
public class CedarFileTypeTest extends BasePlatformTestCase {

    public void testCedarFilesUseTextMate() {
        FileTypeManager ftm = FileTypeManager.getInstance();
        assertEquals("textmate", ftm.getFileTypeByFileName("policy.cedar").getName());
        assertEquals("textmate", ftm.getFileTypeByFileName("agentcore.cedarschema").getName());
        assertEquals("textmate", ftm.getFileTypeByFileName("cedarschema").getName());
    }
}
