# rest-query

Clojure library for translating url into a database query. 

- Powered by [honey.sql](https://github.com/seancorfield/honeysql)
- Inspired by [HL7](https://www.hl7.org/fhir/search.html)

## Motivation

Url query-string is the natural way of querying over REST. Query-string params generally map to underlying data properties and can be handled based on metadata describing those properties. This library converts url + metadata into sql-query.

## Usage

```clojure
(ns user
  (:require
   [rest-query.core :as rq])

(rq/url->query url queryps)

;;  :url
;;
;;  /Person
;;    ?fname=john
;;    &lname=doe
;;    &gender=M
;;    &age=35
;;    &org-name=MyOrg
;;    &sort:desc=created
;;    &page-start=0
;;    &page-size=5
;;
;;  :sql
;;
;;  SELECT person.* 
;;  FROM Person AS person 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE 
;;  INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS resource_age ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS resource_organization ON TRUE 
;;  INNER JOIN Organization AS resource_organization_entity ON CONCAT('/Organization/', resource_organization_entity.id) = CAST(resource_organization AS TEXT) 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource_organization_entity.resource, 'name') AS resource_organization_entity_resource_name ON TRUE 
;;  WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) 
;;   AND (CAST(resource_name_family AS TEXT) LIKE ?) 
;;   AND (CAST(resource_gender AS TEXT) = ?) 
;;   AND (CAST(resource_age AS DECIMAL) = ?) 
;;   AND (CAST(resource_organization_entity_resource_name AS TEXT) LIKE ?) 
;;  ORDER BY created DESC 
;;  LIMIT ? OFFSET ?
```

**Query Params Metadata**

```clojure
(def queryps
  [{:code :filters/text
    :name "fname"
    :path [{:field "resource"}
           {:field "name"}
           {:field "given", :coll true}]}

   {:code :filters/text
    :name "lname"
    :path [{:field "resource"}
           {:field "name"}
           {:field "family"}]}

   {:code :filters/keyword
    :name "gender"
    :path [{:field "resource"}
           {:field "gender"}]}

   {:code :filters/number
    :name "age"
    :path [{:field "resource"}
           {:field "age"}]}

   {:code :filters/text
    :name "org-name"
    :path [{:field "resource"}
           {:field "organization", :link "/Organization/id"}
           {:field "resource"}
           {:field "name"}]}

   {:code :page/sort
    :name "sort"
    :default "created"}

   {:code :page/offset
    :name "page-start"
    :default 0}

   {:code :page/limit
    :name "page-size"
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
| `link`   |         | `{:field "org", :link "/Organization/id"}`                 |

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
| equals                                    | `&age:eq=35`              |
| less than                                 | `&age:lt=2`               |
| less than or equal                        | `&age:le=2`               |
| greater than                              | `&age:gt=21`              |
| greater than or equal                     | `&age:ge=21`              |
| order by descending                       | `&_sort:desc=created`     |
| order by ascending                        | `&_sort=created`          |
|                                           | `&_sort:asc=created`      |
    

## Ideas

- `(util/gen-queryps map)`: generate queryps from sample data map in order to help user creating queryp metadata
- `(rq/url->query url {:queryps queryps, :op-map op-map})`: ability to provide `operators` mapping, so user can use custom operators 
