# rest-query

Clojure library for translating REST url query-string into a database query. Powered by [honey.sql](https://github.com/seancorfield/honeysql).

## Motivation

Url query-string is the accurate way to query over restful apis. Query-string params usually map to underlying data properties. Query-string properties generally can be handled based on the type of data, for instance: text search, keyword exact matching, number range, date range, etc. Converting query parameters into sql-query is a repetitive task that can be abstracted and optimized by providing users with a way to generate, export, and replace queries. These are the aims of this library.

## Usage

```clojure
(ns user
  (:require
   [rest-query.core :as rq])

(rq/url->query "/Person?fname=john&lname=doe&gender=M&age=35&_offset=0&_limit=5" queryps)

;; =>
;; {:from :Person
;;  :hash "6c1dd451d83d6b1bbf32c3be55115785492a8cd4fbf073ef719a331869f6c370"
;;  :page ["SELECT res.*
;;          FROM Person AS res 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE 
;;          INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS age ON TRUE 
;;          WHERE (CAST(fname AS TEXT) LIKE ?) 
;;            AND (CAST(lname AS TEXT) LIKE ?) 
;;            AND (CAST(gender AS TEXT) = ?) 
;;            AND (CAST(age AS DECIMAL) = ?) 
;;          LIMIT ? OFFSET ?"
;;          "name" "given" "family" "gender" "age" "%john%" "%doe%" "\"M\"" 35M 5 0]
;;  :total ["SELECT COUNT(*) AS count
;;          FROM Person AS res 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE 
;;          INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE 
;;          INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS age ON TRUE 
;;          WHERE (CAST(fname AS TEXT) LIKE ?) 
;;            AND (CAST(lname AS TEXT) LIKE ?) 
;;            AND (CAST(gender AS TEXT) = ?) 
;;            AND (CAST(age AS DECIMAL) = ?)"
;;          "name" "given" "family" "gender" "age" "%john%" "%doe%" "\"M\"" 35M]}
```

**queryps**

```clojure
(def queryps
  [{:name :fname, :code :filters/text, :path [{:field "resource"} {:field "name"} {:field "given", :coll true}]}
   {:name :lname, :code :filters/text, :path [{:field "resource"} {:field "name"} {:field "family"}]}
   {:name :gender, :code :filters/keyword, :path [{:field "resource"} {:field "gender"}]},
   {:name :age, :code :filters/number, :path [{:field "resource"} {:field "age"}]},
   {:name :_offset :code :page/offset :default 0}
   {:name :_limit, :code :page/limit, :default 128}])
```

**:Person**

```
| :id | :resource                    |
|-----|------------------------------|
| 1   | {                            |
|     |   "type": "Person",          |
|     |   "name": {                  |
|     |     "given": ["John", "M."], |
|     |     "family": "Doe"},        |
|     |   },                         |
|     |   "gender": "M"              |
|     | }                            |
|     |                              |
```

## Reference [wip]

### Fns

``` clojure
(rq/url->query url queryps)

Convert url string into a query map.
```

``` clojure
(rq/make-query url-map queryps)

Convert url-map string into a query map. 
Convenience overload for cases where url 
has been already parsed, by example: in 
a ring handler.
```

``` clojure
(rq/make-sql-map url-map queryps)

Convert url-map into a honey.sql sql-map 
for manipulation.
```

### Path

| key      | required | default | example                                                   |
|----------|----------|---------|-----------------------------------------------------------|
| `prop`   | yes      |         | `{:prop "name"}`                                          |
| `coll`   | no       | `false` | `{:prop "contacts", :coll true}`                          |
| `filter` | no       | `{}`    | `{:prop "contacts", :coll true, :filter {:type "email"}}` |
|          |          |         |                                                           |

### Filters

| code               | query-string  | translated to                            | status  |
|--------------------|---------------|------------------------------------------|---------|
| `:filters/text`    | `&name=john`  | `WHERE CAST(name AS TEXT) like '%john%'` | ready   |
| `:filters/keyword` | `&gender=M`   | `WHERE CAST(gender AS TEXT) = 'M'`       | ready   |
| `:filters/url`     |               |                                          | planned |
| `:filters/number`  | `&age=35`     | `WHERE CAST(age AS DECIMAL) = 35`        | partial |
| `:filters/date`    |               |                                          | planned |
| `:page/offset`     | `&_offset=0`  | `OFFSET 0`                               | ready   |
| `:page/limit`      | `&_limit=128` | `LIMIT 128`                              | ready   |
|                    |               |                                          |         |

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
|                                           |                           |
    
