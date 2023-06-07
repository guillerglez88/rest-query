# rest-query

Clojure library for translating url into a database query. 

- Powered by [honey.sql](https://github.com/seancorfield/honeysql)
- Inspired by [HL7](https://www.hl7.org/fhir/search.html)

## Motivation

Url query-string is the natural way of querying over REST. Query-string params usually map to underlying data properties and can be handled based on the properties types, for instance: string -> text search, number -> comparison, date -> interval comparison, etc. Converting query params into sql-query is a task that can be abstracted and optimized by providing users with a way to generate, export, and replace queries. These are the aims of this library.

## Usage

```clojure
(ns user
  (:require
   [rest-query.core :as rq])

(sut/url->query "/Person?fname=john&lname=doe&gender=M&age=35&org-name=MyOrg&_sort=desc:created&_offset=0&_limit=5" queryps)

;; => {:from :Person
;;     :hash "9ec9fa56f228c638e5554b3fd73c9426adcc86291af73e6d6371b088c5972ff7"
;;     :page [(str "SELECT person.* "
;;                 "FROM Person AS person "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
;;                 "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS org ON TRUE "
;;                 "INNER JOIN Organization AS org_entity ON CONCAT('/Organization/', org_entity.id) = CAST(org AS TEXT) "
;;                 "INNER JOIN JSONB_EXTRACT_PATH(org_entity, 'name') AS org_name ON TRUE "
;;                 "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
;;                   "AND (CAST(lname AS TEXT) LIKE ?) "
;;                   "AND (CAST(gender AS TEXT) = ?) "
;;                   "AND (CAST(age AS DECIMAL) = ?) "
;;                    "AND (CAST(org_name AS TEXT) LIKE ?) "
;;                 "ORDER BY created DESC "
;;                 "LIMIT ? OFFSET ?")
;;            "%john%" "%doe%" "\"M\"" 35M "%MyOrg%" 5 0]
;;     :total [(str "SELECT COUNT(*) AS count "
;;                  "FROM Person AS person "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
;;                  "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS org ON TRUE "
;;                  "INNER JOIN Organization AS org_entity ON CONCAT('/Organization/', org_entity.id) = CAST(org AS TEXT) "
;;                  "INNER JOIN JSONB_EXTRACT_PATH(org_entity, 'name') AS org_name ON TRUE "
;;                  "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
;;                    "AND (CAST(lname AS TEXT) LIKE ?) "
;;                    "AND (CAST(gender AS TEXT) = ?) "
;;                    "AND (CAST(age AS DECIMAL) = ?) "
;;                     "AND (CAST(org_name AS TEXT) LIKE ?) "
;;                  "ORDER BY created DESC")
;;             "%john%" "%doe%" "\"M\"" 35M "%MyOrg%"]}
```

**Query Params Metadata**

```clojure
(def queryps
  [{:code :filters/text
    :path [{:field "resource"}
           {:field "name"}
           {:field "given", :coll true, :alias "fname"}]}

   {:code :filters/text
    :path [{:field "resource"}
           {:field "name"}
           {:field "family", :alias "lname"}]}

   {:code :filters/keyword
    :path [{:field "resource"}
           {:field "gender", :alias "gender"}]},

   {:code :filters/number
    :path [{:field "resource"}
           {:field "age", :alias "age"}]},

   {:code :filters/text
    :path [{:field "resource"}
           {:field "organization", :link "/Organization/id", :alias "org"}
           {:field "name", :alias "org-name"}]}

   {:code :page/sort
    :path [{:alias "_sort"}]
    :default "created"}

   {:code :page/offset
    :path [{:alias "_offset"}]
    :default 0}

   {:code :page/limit
    :path [{:alias "_limit"}]
    :default 128}])
```

**Postgres database tables**

```
#Person
| id | resource                            |
|----|-------------------------------------|
| 1  | {                                   |
|    |   "type": "Person",                 |
|    |   "name": {                         |
|    |     "given": ["John", "M."],        |
|    |     "family": "Doe"},               |
|    |   },                                |
|    |   "gender": "M",                    |
|    |   "organization": "/Organization/1" |
|    | }                                   |

#Organization
| id | name             |
|----|------------------|
| 1  | "MyOrganization" |
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
