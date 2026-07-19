package cronh

import cronh.dsl.aliases.AllAliases

/** Public imports for cronh's scheduling DSL.
  *
  * A single `import cronh.dsl.*` brings the DSL entry point, literals, aliases,
  * and range syntax into scope.
  */
package object dsl {
  export AllAliases.*
}
