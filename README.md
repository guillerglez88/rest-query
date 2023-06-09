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
;;  INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE 
;;  INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE 
;;  INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS org ON TRUE 
;;  INNER JOIN Organization AS org_entity ON CONCAT('/Organization/', org_entity.id) = CAST(org AS TEXT) 
;;  INNER JOIN JSONB_EXTRACT_PATH(org_entity.resource, 'name') AS org_name ON TRUE 
;;  WHERE (CAST(fname_elem AS TEXT) LIKE ?) 
;;    AND (CAST(lname AS TEXT) LIKE ?) 
;;    AND (CAST(gender AS TEXT) = ?) 
;;    AND (CAST(age AS DECIMAL) = ?) 
;;    AND (CAST(org_name AS TEXT) LIKE ?) 
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
           {:field "given", :coll true, :alias "fname"}]}

   {:code :filters/text
    :name "lname"
    :path [{:field "resource"}
           {:field "name"}
           {:field "family", :alias "lname"}]}

   {:code :filters/keyword
    :name "gender"
    :path [{:field "resource"}
           {:field "gender", :alias "gender"}]}

   {:code :filters/number
    :name "age"
    :path [{:field "resource"}
           {:field "age", :alias "age"}]}

   {:code :filters/text
    :name "org-name"
    :path [{:field "resource"}
           {:field "organization", :link "/Organization/id", :alias "org"}
           {:field "resource"}
           {:field "name", :alias "org-name"}]}

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
|    |   "age": 35,                        |
|    |   "organization": "/Organization/1" |
|    | }                                   |

#Organization
| id | name             |
|----|------------------|
| 1  | "MyOrganization" |

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

The `path` is the route to the property inside the table, starting with the table column and continuing with each property in deep until the desired property is reached. Can filter resources based on referenced entities by specifying `:link "/<table>/<column>"` in the path component, the convention for references is: `/<type>/<id>`. Linked entities can be used only for narrowing results not for bringing additional data as part of the principal resource. By default, an alias is generated concatenating field names, a custom alias can be specified though.

| key      | required | default | example                                                    |
|----------|----------|---------|------------------------------------------------------------|
| `field`  | `yes`    |         | `{:field "name"}`                                          |
| `coll`   |          | `false` | `{:field "contacts", :coll true}`                          |
| `filter` |          |         | `{:field "contacts", :coll true, :filter {:type "email"}}` |
| `alias`  |          |         | `{:field "firstName", :alias :fname}`                      |
| `link`   |          |         | `{:field "org", :link "/Organization/id"}`                 |

### Filters

The query parameter should be marked with a `:code`, the value of this code instructs the library to cast, filter, order and limit results accordingly.

| code               | query-string     | translated to                            | status  |
|--------------------|------------------|------------------------------------------|---------|
| `:filters/text`    | `&name=john`     | `WHERE CAST(name AS TEXT) like '%john%'` | ready   |
| `:filters/keyword` | `&gender=M`      | `WHERE CAST(gender AS TEXT) = 'M'`       | ready   |
| `:page/sort`       | `&sort=created`  | `ORDER BY created`                       | ready   |
| `:filters/number`  | `&age=35`        | `WHERE CAST(age AS DECIMAL) = 35`        | partial |
| `:page/offset`     | `&page-start=0`  | `OFFSET 0`                               | ready   |
| `:page/limit`      | `&page-size=128` | `LIMIT 128`                              | ready   |
| `:filters/url`     |                  |                                          | planned |
| `:filters/date`    |                  |                                          | planned |

### Operators

Depending on the type of data being filtered, an operation may be desirable in order to adjust data manipulation behaviour. The operation syntax convention is: `<param>:<op>=<val>` where `<op>` is the operation `code`.

| code   | desc                  | example              |
|--------|-----------------------|----------------------|
| `eq`   | equals                | `&age:eq=35`         |
| `lt`   | less than             | `&age:lt=2`          |
| `le`   | less than or equal    | `&age:le=2`          |
| `gt`   | greater than          | `&age:gt=21`         |
| `ge`   | greater than or equal | `&age:ge=21`         |
| `desc` | order by descending   | `&sort:desc=created` |
| `asc`  | order by ascending    | `&sort:asc=created`  |
    

## Ideas

- `(util/gen-queryps map)`: generate queryps from sample data map in order to help user creating queryp metadata
- `(rq/url->query url {:queryps queryps, :op-map op-map})`: ability to provide `operators` mapping, so user can use custom operators 
