# rest-query

Clojure library for translating url into a database query. Powered by [honey.sql](https://github.com/seancorfield/honeysql). Inspired by [HL7](https://www.hl7.org/fhir/search.html).

## Motivation

Url query-string is the way to query over restful apis. Query-string params usually map to underlying data properties. Query-string params generally can be handled based on its type, for instance: text search, keyword exact matching, number range, date range, etc. Converting query params into sql-query is a repetitive task that can be abstracted and optimized by providing users with a way to generate, export, and replace queries. These are the aims of this library.

## Usage

```clojure
(ns user
  (:require
   [rest-query.core :as rq])

(rq/url->query "/Person?fname=john&lname=doe&gender=M&age=35&_sort=desc:created&_offset=0&_limit=5" queryps)

;; =>
;; {:from :Person
;;  :hash "432db868f994fa4d79e48ee35fae6fb3ddabb531ff56835bd1fcda6ab37d5238"
;;  :page ["SELECT person.*
;;          FROM Person AS person 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE 
;;          INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE 
;;          WHERE (CAST(fname AS TEXT) LIKE ?) 
;;            AND (CAST(lname AS TEXT) LIKE ?) 
;;            AND (CAST(gender AS TEXT) = ?) 
;;            AND (CAST(age AS DECIMAL) = ?) 
;;          ORDER BY created DESC
;;          LIMIT ? OFFSET ?"
;;          "%john%" "%doe%" "\"M\"" 35M 5 0]
;;  :total ["SELECT COUNT(*) AS count
;;           FROM Person AS res 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE 
;;           INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE 
;;           WHERE (CAST(fname AS TEXT) LIKE ?) 
;;             AND (CAST(lname AS TEXT) LIKE ?) 
;;             AND (CAST(gender AS TEXT) = ?) 
;;             AND (CAST(age AS DECIMAL) = ?)
;;           ORDER BY created DESC"
;;          "%john%" "%doe%" "\"M\"" 35M]}
```

**Query Params Metadata**

```clojure
(def queryps
  [{:code :filters/text
    :path [{:field "resource"}
           {:field "name"}
           {:field "given", :coll true, :alias :fname}]}
   {:code :filters/text
    :path [{:field "resource"}
           {:field "name"}
           {:field "family", :alias :lname}]}
   {:code :filters/keyword
    :path [{:field "resource"}
           {:field "gender", :alias :gender}]},
   {:code :filters/number
    :path [{:field "resource"}
           {:field "age", :alias :age}]},
   {:code :page/sort
    :path [{:alias :_sort}]
    :default "created"}
   {:code :page/offset
    :path [{:alias :_offset}]
    :default 0}
   {:code :page/limit
    :path [{:alias :_limit}]
    :default 128}])
```

**Postgres database table**

```
| id | resource                     |
|----|------------------------------|
| 1  | {                            |
|    |   "type": "Person",          |
|    |   "name": {                  |
|    |     "given": ["John", "M."], |
|    |     "family": "Doe"},        |
|    |   },                         |
|    |   "gender": "M"              |
|    | }                            |
```

## Reference [wip]

### Fns

``` clojure
(rq/url->query url queryps)

;; Convert url string into a query map.
```

``` clojure
(rq/make-query url-map queryps)

;; Convert url-map string into a query map. 
;; Convenience overload for cases where url 
;; has been already parsed, by example: in 
;; a ring handler.
```

``` clojure
(rq/make-sql-map url-map queryps)

;; Convert url-map into a honey.sql sql-map 
;; for manipulation.
```

### Path

| key      | default | example                                                    |
|----------|---------|------------------------------------------------------------|
| `field`  |         | `{:field "name"}`                                          |
| `coll`   | `false` | `{:field "contacts", :coll true}`                          |
| `filter` |         | `{:field "contacts", :coll true, :filter {:type "email"}}` |
| `alias`  |         | `{:field "firstName", :alias :fname}`                      |

### Filters

| code               | query-string     | translated to                            | status  |
|--------------------|------------------|------------------------------------------|---------|
| `:filters/text`    | `&name=john`     | `WHERE CAST(name AS TEXT) like '%john%'` | ready   |
| `:filters/keyword` | `&gender=M`      | `WHERE CAST(gender AS TEXT) = 'M'`       | ready   |
| `:filters/url`     |                  |                                          | planned |
| `:filters/number`  | `&age=35`        | `WHERE CAST(age AS DECIMAL) = 35`        | partial |
| `:filters/date`    |                  |                                          | planned |
| `:page/sort`       | `&_sort=created` | `ORDER BY created`                       | ready   |
| `:page/offset`     | `&_offset=0`     | `OFFSET 0`                               | ready   |
| `:page/limit`      | `&_limit=128`    | `LIMIT 128`                              | ready   |

### Operators

| op                                        | example                   |
|-------------------------------------------|---------------------------|
| equals                                    | `&age=eq:35`              |
| less than                                 | `&age=lt:2`               |
| less than or equal                        | `&age=le:2`               |
| greater than                              | `&age=gt:21`              |
| greater than or equal                     | `&age=ge:21`              |
| escape `:` to be interpreted as delimiter | `&time=esc:8:00-17:30`    |
|                                           | `&desc=esc:foo%20bar:baz` |
| order by descending                       | `&_sort=desc:created`     |
| order by ascending                        | `&_sort=created`          |
|                                           | `&_sort=asc:created`      |
    

## Ideas

- `(util/gen-queryps map)`: generate queryps from sample data map in order to help user creating queryp metadata
- `(rq/url->query url {:queryps queryps, :op-map op-map})`: ability to provide `operators` mapping, so user can use custom operators 
- `{:field "...", :link "EntityType"}`: ability to filter by `EntityType` props after joining with first entity so a cross entities path can be constructed.
