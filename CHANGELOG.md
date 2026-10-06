<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Lucene Query Injection Companion Changelog

## [Unreleased]

### Added

- A description page for the inspection in **Settings | Editor |
  Inspections**, which showed "Under construction".

### Changed

- The rating prompt's local counter keeps one-way fingerprints of findings
  instead of their file paths, and deletes the list that earlier versions
  kept.
- `PRIVACY.md` describes the values the plugin keeps in the IDE's local
  settings.

## [0.1.1]

### Fixed

- Review/star CTA now links to this plugin's own Marketplace
  reviews page instead of the vendor's generic plugin list.

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

[Unreleased]: https://github.com/GapHunterLabs/lucene-query-injection-companion/compare/0.1.1...HEAD
[0.1.1]: https://github.com/GapHunterLabs/lucene-query-injection-companion/compare/0.1.0...0.1.1
[0.1.0]: https://github.com/GapHunterLabs/lucene-query-injection-companion/commits/0.1.0
