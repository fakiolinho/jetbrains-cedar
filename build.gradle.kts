import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.changelog") version "2.5.0" // only for markdownToHTML
}

group = "io.github.fakiolinho"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Build against Community; the plugin only touches platform APIs so it runs in PyCharm, IDEA, etc.
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion")) { useInstaller = false }
        bundledPlugin("org.jetbrains.plugins.textmate")
        plugin("com.redhat.devtools.lsp4ij:${providers.gradleProperty("lsp4ijVersion").get()}")
        pluginVerifier()
        zipSigner()
    }
    testImplementation("junit:junit:4.13.2")
}

// The IntelliJ plugin defaults the toolchain to the platform's Java (21); compile with whatever JDK runs Gradle
// instead, so the CI matrix really exercises each JDK. 2024.2 platform jars are Java 21 bytecode, so that's the floor.
java {
    toolchain { languageVersion = JavaLanguageVersion.of(JavaVersion.current().majorVersion) }
}

tasks.withType<JavaCompile> {
    options.release = 17 // bytecode level for 2024.2+; needs JDK >= 21 to build
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null } // no upper bound; the verifier task catches real breakage
        }
        // The IDE renders change notes as HTML, not Markdown. Version headings link to their GitHub release.
        changeNotes = providers.fileContents(layout.projectDirectory.file("CHANGELOG.md")).asText.map { md ->
            val repoUrl = "https://github.com/fakiolinho/jetbrains-cedar" // local: a script val breaks the config cache
            markdownToHTML(md.replace(Regex("^## (\\S+)$", RegexOption.MULTILINE)) {
                val v = it.groupValues[1]
                "## [$v]($repoUrl/releases/tag/v$v)"
            })
        }
    }
    pluginVerification {
        ides { recommended() }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks.test { useJUnit() }
