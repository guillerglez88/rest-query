# rest-query

Clojure library for translating REST url query-string into a database query. Powered by [honey.sql](https://github.com/seancorfield/honeysql).

## Motivation

Url query-string is the accurate way to query over restful apis. Query-string params usually map to underlying data properties. Query-string properties generally can be handled based on the type of data, for instance: text search, keyword exact matching, number range, date range, etc. Converting query parameters into sql-query is a repetitive task that can be abstracted and optimized by providing users with a way to generate, export, and replace queries. These are the aims of this library.

## Usage

```clj
(ns user
  (:require
   [rest-query.core :as rq])

(rq/url->query "/Person?fname=john&lname=doe&gender=M&_offset=0&_limit=5" :resource queryps)

;; =>
;;  {:from :Person
;;   :page ["SELECT res.* 
;;           FROM Person AS res 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE 
;;           INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE 
;;           WHERE (CAST(fname AS text) LIKE ?) 
;;               AND (CAST(lname AS text) LIKE ?) 
;;               AND (CAST(gender AS text) = ?) LIMIT ? OFFSET ? 
;;           LIMIT ? OFFSET ?"
;;          "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 5 0]
;;   :total ["SELECT COUNT(*) AS count 
;;            FROM Person AS res 
;;            ..." ;; omited for conciseness, same query as before, without paging
;;           "name" "given" "family" "gender" "%john%" "%doe%" "\"M\""]}
```

**queryps**

``` edn
[{:name :fname   :code :filters/text                    :path [{:prop "name"} {:prop "given" :coll true}]}
 {:name :lname   :code :filters/text                    :path [{:prop "name"} {:prop "family"}]}
 {:name :gender  :code :filters/keyword                 :path [{:prop "gender"}]}
 {:name :_offset :code :page/offset     :default 0      :path []}
 {:name :_limit  :code :page/limit      :default 128    :path []}])
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
(rq/url->query url base queryps)

Convert url string into a query map.
```

``` clojure
(rq/make-query url-map base queryps)

Convert url-map string into a query map. 
Convenience overload for cases where url 
has been already parsed, by example: in 
a ring handler.
```

``` clojure
(rq/make-sql-map url-map base queryps)

Convert url-map into a honey.sql sql-map 
for manipulation.
```

### Queryp codes

| code               | translated to        | desc               | status      |
|--------------------|----------------------|--------------------|-------------|
| `:filters/text`    | `name like '%john%'` | contains text      | implemented |
| `:filters/keyword` | `name = 'john'`      | match exact        | implemented |
| `:filters/url`     |                      |                    | planned     |
| `:filters/number`  | `age = 35`           |                    | partial     |
| `:filters/date`    |                      |                    | planned     |
| `:page/offset`     | `OFFSET 0`           | page starting item | implemented |
| `:page/limit`      | `LIMIT 128`          | page size          | implemented |
|                    |                      |                    |             |
