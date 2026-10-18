## 0.1.5

- The repository now has a contributing guide (`CONTRIBUTING.md`) and a code of conduct (`CODE_OF_CONDUCT.md`),
  adapted from vscode-cedar. No changes to the plugin itself.

## 0.1.4

- `.cedar` and `.cedarschema` files show the Cedar icons from vscode-cedar instead of TextMate's generic icon.

## 0.1.3

- When a policy has no `*.cedarschema` nearby, a notification explains that only syntax is checked and where to put
  a schema. "Choose schema…" picks one for that file and for later files in the project that have none nearby.
- `.cedar` and `.cedarschema` are declared as file extensions of the plugin, so the IDE can suggest it when someone
  opens a Cedar file without it installed (once the plugin is on the Marketplace).

## 0.1.2

- The IDE now asks for a restart after installing or updating the plugin. Loading it without one left `.cedar`
  files without syntax highlighting.
- "What's New" is rendered as HTML instead of raw Markdown, and each version links to its GitHub release.
- The plugin description links to the source, issues, releases, license and author.

## 0.1.1

- A missing `cargo` or a failed server install now shows one notification instead of flooding the IDE error
  log with LSP4IJ exceptions from every editor feature.

## 0.1.0

- Syntax highlighting for `.cedar` and `.cedarschema` (grammar from cedar-policy/vscode-cedar).
- Validation, completion, hover, go to definition, rename, folding, formatting via `cedar-language-server`.
- Automatic schema association: nearest `*.cedarschema` or `*.cedarschema.json` up the tree.
- "Click to associate schema" code lens opens a file chooser.
- One click install of the server with cargo when it is missing.
