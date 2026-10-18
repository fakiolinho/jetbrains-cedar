# CLAUDE.md

JetBrains IDE plugin adding Cedar policy language support. It reimplements nothing: highlighting is the vendored
vscode-cedar TextMate grammar, language features come from the official `cedar-language-server` through LSP4IJ.
The only real logic is schema association, which the server leaves to the client.

## Build

```sh
./gradlew test buildPlugin     # zip lands in build/distributions/
./gradlew verifyPlugin         # downloads ~8 IDEs (~4 GB); leave it to CI unless disk is plentiful
```

- Needs JDK 21+ to run Gradle. On the maintainer's Mac there is no system JDK; use a JetBrains IDE's bundled runtime:
  `export JAVA_HOME=~/Applications/PyCharm.app/Contents/jbr/Contents/Home` (Java 25).
- The build compiles with whatever JDK runs Gradle (toolchain override in `build.gradle.kts`), because the IntelliJ
  Platform Gradle Plugin otherwise demands a separate Java 21 install. CI tests on 21 and 25.
- `useInstaller = false` on the platform dependency is deliberate: the macOS `.dmg` extraction path breaks, and the
  Maven artifact is smaller.
- Bytecode targets 17 (`options.release`), but the 2024.2 platform jars are Java 21 bytecode, so JDK 17 can't build.

## Layout (`src/main/java/io/github/fakiolinho/cedar/`)

- `CedarTextMateBundleProvider`: unpacks `resources/textmate/cedar/` into the IDE system dir for the TextMate plugin.
- `CedarConnectionProvider`: starts the server over stdio; on each `textDocument/didOpen` of a policy/entities file it
  sends `cedar.associateSchema` with the nearest schema. Server cwd is a temp dir because it writes `./logs/`.
- `SchemaFinder`: pure logic for "nearest `*.cedarschema` up the tree, never above the project root". Unit tested.
- `SchemaOptionsAction`: backs the server's "Click to associate schema" code lens. Its action id in `plugin.xml` must
  equal the LSP command name (`cedar.schemaOptions`).
- `CedarServerInstaller`: `cargo install`s the server when missing (upstream ships no binaries). `Binaries` checks
  `~/.cargo/bin` explicitly since Dock-launched IDEs get a bare PATH.

## Rules

- `verifyPlugin` fails CI on internal API usage; avoid `@ApiStatus.Internal` APIs (e.g. `PluginManagerCore`) and
  prefer non-deprecated APIs that exist back to `pluginSinceBuild` (242).
- The plugin id `io.github.fakiolinho.cedar` is published and must never change.

## Releasing

1. Bump `pluginVersion` in `gradle.properties` and add a `## <version>` section to `CHANGELOG.md` (it becomes both the
   IDE change notes and the GitHub release notes).
2. `git tag v<version> && git push origin main v<version>`. The tag must match `pluginVersion` or the job fails.
3. CI signs the plugin and attaches `jetbrains-cedar-<version>.zip` to a GitHub release. It publishes to the
   Marketplace only when the repo variable `MARKETPLACE_PUBLISH` is `true` (set after the first, manual, upload).

Signing and publishing read `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN` from repo
secrets. Don't commit key material.
