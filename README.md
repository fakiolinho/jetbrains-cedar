# Cedar Policy Language for JetBrains IDEs

Syntax highlighting, validation, schema driven completion, hover, go to definition, rename and formatting for
[Cedar](https://www.cedarpolicy.com) policies in PyCharm, IntelliJ IDEA, WebStorm, GoLand and the rest of the family.

Nothing here is reimplemented. Highlighting is the TextMate grammar from
[cedar-policy/vscode-cedar](https://github.com/cedar-policy/vscode-cedar); language intelligence is the official
[`cedar-language-server`](https://github.com/cedar-policy/cedar/tree/main/cedar-language-server) wired in through
[LSP4IJ](https://github.com/redhat-developer/lsp4ij). The plugin adds the one piece that lives in the VS Code extension
rather than the server: deciding which schema a policy is validated against.

## Install

1. Marketplace: *Settings → Plugins → Marketplace → "Cedar Policy Language"*. LSP4IJ is pulled in as a dependency.
2. A Rust toolchain ([rustup.rs](https://rustup.rs)). On first open of a `.cedar` file the plugin offers to run
   `cargo install --git https://github.com/cedar-policy/cedar cedar-language-server --locked`. Upstream ships no
   prebuilt binaries yet. If you already have the server on `PATH` or in `~/.cargo/bin`, it is used as is.

## Schema association

Policies (`*.cedar`) and entities (`*.cedarentities.json`) are validated against the nearest `*.cedarschema` or
`*.cedarschema.json` found walking up from the file to the project root. Without a schema you still get syntax
diagnostics, formatting and keyword completion; with one you get entity types, actions, attributes and
`context.input.*` per action.

To pick a schema by hand, click the *Click to associate schema* code lens at the top of the file.

`examples/` has an AgentCore Gateway schema and a policy that validates against it.

## Build from source

```bash
./gradlew buildPlugin      # build/distributions/jetbrains-cedar-<version>.zip
./gradlew runIde           # sandboxed IDE with the plugin
./gradlew verifyPlugin     # binary compatibility against the recommended IDE set
```

Install the zip through *Settings → Plugins → ⚙ → Install Plugin from Disk*.

## Credits

Grammar and language configuration: Cedar Contributors, Apache 2.0 (see `src/main/resources/textmate/cedar/LICENSE`).
