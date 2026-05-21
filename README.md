# html2hiccup

HTML->hiccup transformer, a la Calva, but as a babashka task.

## Usage

`html2hiccup [options]`
`html2hiccup [options] file1.html file2.htm ...`

### No files

- Reads HTML from `stdin`
- Writes hiccup to `stdout`

### With files

- Reads supplied files
- Writes sibling .edn files

### Options

- `--add-classes-to-tag-keyword` `true`|`false` (default: `true`)
- `--kebab-attrs` `true`|`false` (default: `false`)
- `--mapify-style` `true`|`false` (default: `false`)
