FHIRPLACE

Internal format is “JSON”.
Should validate before insert, update.

Features:

CREATE
  xml support

READ
  xml support

UPDATE
 * implement version-aware updates
 * create non-existing resource REJECTION REASON: (not campatible with version-aware updates)
 _ xml support

DELETE
 * delete already deleted resource cause no effects and return 204.
 * refuses to delete because of referential integrity (409)
 * Many resources have a status element that overlaps with the idea of deletion (should be done on FHIRPLACE or FHIRBASE?)
  xml support

VREAD
  xml support

HISTORY (3 types: instance, resource, all)
 * may be added additional fields (_since, _count)
  xml support
  Optional Paging
  Optional rss/atom

Validation
  Not done at all
  xml support

Search
  Not done at all
  type level
  system level
  xml support

Errors
  Return errors from any action
  xml support

Transactions
  Not done at all
  xml support

Tags
  Not done at all
  xml support

Conformance
  xml support

HTML Interface
