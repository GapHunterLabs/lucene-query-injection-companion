<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Lucene Query Injection Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- Hand-written Lucene query-string tokenizer + recursive-descent
  parser (this catalog's fifth full custom grammar, with range/boost/
  fuzzy operators none of the other four have).
- Same-method taint detection from an HTTP endpoint parameter to
  `QueryBuilders.queryStringQuery(...)`/`.simpleQueryStringQuery(...)`
  (CWE-943/CWE-400), with a bare tainted reference flagged
  unconditionally and a concatenation's static skeleton validated
  against the grammar as noise reduction.

[Unreleased]: https://github.com/GapHunterLabs/lucene-query-injection-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/lucene-query-injection-companion/commits/0.1.0
