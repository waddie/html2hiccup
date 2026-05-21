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
- Writes sibling `.edn` files

### Options

- **`--add-classes-to-tag-keyword`** `true`|`false` (default: `true`)

  Controls how CSS classes are handled in the output:

  - **`true`**: Classes that can be valid Clojure keywords are attached directly
    to the tag using hiccup's dot notation (e.g., `<div class="btn primary">`
    becomes `[:div.btn.primary]`)

  - **`false`**: All classes remain in a `:class` attribute as a vector (e.g.,
    `[:div {:class ["btn" "primary"]}]`)

  Classes containing special characters (like hyphens, dots, or starting with
  numbers) that can't be keywords are always placed in the `:class` attribute
  regardless of this setting.

- **`--kebab-attrs`** `true`|`false` (default: `false`)

  Controls the casing of HTML attribute names in the output:

  - **`true`**: Attribute names are converted to kebab-case using the
    `camel-snake-kebab` library (e.g., `dataFooBar` → `:data-foo-bar`, `innerHTML`
    → `:inner-html`)

  - **`false`**: Attribute names are lowercased (e.g., `dataFooBar`
    → `:datafoob`)

  Special cases like `baseprofile` → `:base-profile` and `viewbox` → `:view-box`
  are always handled correctly regardless of this setting.

- **`--mapify-style`** `true`|`false` (default: `false`)

  Controls how the CSS `style` attribute is represented:

  - **`true`**: The style string is parsed and converted to a map with Clojure
    keywords as keys (e.g., `style="color: red; font-size: 14px"` becomes `{:style
{:color "red" :font-size 14.0}}`). Numeric values are converted to numbers, and
    alphabetic values become keywords.

  - **`false`**: The style remains as a CSS string (e.g., `{:style "color: red;
font-size: 14px"}`)
