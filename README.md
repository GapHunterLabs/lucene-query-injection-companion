# Lucene Query Injection Companion

Flags a `QueryBuilders.queryStringQuery(...)`/`.simpleQueryStringQuery(...)`
call whose argument is built from an HTTP endpoint parameter.

## Why it exists

CWE-943 (an attacker's raw input becomes Lucene query SOURCE TEXT,
enabling filter bypass) combined with CWE-400: CVE-2023-31419 confirms
a real Denial of Service via a maliciously crafted query string
against Elasticsearch's own `_search` API (Stack Overflow from deeply
nested query syntax). Searched explicitly for a CodeQL/Datadog rule
for this exact angle and found none (unlike SQL injection, which both
cover extensively). No dedicated Marketplace plugin found.

## Why built this way

- **A real hand-written Lucene query-string parser** -- the FIFTH full
  custom grammar in this catalog, after regex
  (`redos-catastrophic-backtracking-companion`), SpEL
  (`spel-injection-sink-companion`), XPath
  (`xpath-injection-sink-companion`), and LDAP
  (`ldap-injection-sink-companion`). Introduces range queries
  (`[1 TO 10]`/`{1 TO 10}`), boosting (`term^2.0`), and fuzzy matching
  (`term~2`) -- operators none of the other four grammars have,
  genuinely new parsing rules rather than a variant of an existing
  pattern.
- **A bare tainted reference is flagged unconditionally, with no
  skeleton to validate** -- confirmed necessary the hard way while
  building `ldap-injection-sink-companion`: gating the "whole argument
  is the tainted parameter" case on skeleton validity produced a false
  negative on the single MOST dangerous shape (a lone placeholder has
  no structure to check against). The grammar validates a
  concatenation's static skeleton only -- noise reduction, never a
  security gate.

## v0.1 scope — stated honestly, not exhaustively

- Only `org.elasticsearch.index.query.QueryBuilders`, checked by the
  qualifier's own reference TEXT, never resolved against the real
  Elasticsearch classpath.
- Only same-method taint (a direct reference, or a `+` concatenation
  of literals and tainted references).
- A term/identifier does not include `-` in its own character class
  (a hyphenated term like `field-name` is out of scope, since `-` is
  also the real "prohibit this clause" prefix operator).
- A numeric-typed endpoint parameter (`int`/`Integer`/`long`/`Long`/
  `double`/`Double`/`float`/`Float`/`short`/`Short`/`byte`/`Byte`) is
  never treated as a taint source -- a genuinely numeric value can
  never carry Lucene injection syntax, and this grammar leans heavily
  on numeric-looking operands (range/boost/fuzzy), so without this
  exclusion a legitimate `score:[minScore TO 100]`-style range would
  false-positive.

## Usage

Open a Java file with a Spring MVC/JAX-RS endpoint method that builds
a Lucene/Elasticsearch query from a request parameter -- the
`queryStringQuery(...)`/`simpleQueryStringQuery(...)` call shows a
warning.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
