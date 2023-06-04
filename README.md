# rest-query

Clojure library for translating REST url query-string into a database query

## Desc [wip]

Ability to convert raw URL and query-string params metadata into SQL query. By default, pg-sql is being targeted but the client code is provided with a hash, unique for each specific query, not the parameter values, so the client code can optimize the generated query by mapping the query based on the provided hash. This also makes it possible, porting queries to a different database or even a different query language. Honey.sql is being used under the hood.

## Reference [wip]

```clj
(url->query "/Person?fname=john&lname=doe&gender=M&_offset=0&_limit=5" 
            :resource 
            [{:name :fname     :code :filters/text    :path [{:name "name"} 
                                                             {:name "given" 
                                                              :collection true}]}
             {:name :lname     :code :filters/text    :path [{:name "name"} 
                                                             {:name "family"}]}
             {:name :gender    :code :filters/keyword :path [{:name "gender"}]}
             {:name :_offset   :code :page/offset     :path []                      :default 0}
             {:name :_limit    :code :page/limit      :path []                      :default 128}])

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
;;               AND (CAST(gender AS text) = ?) LIMIT ? OFFSET ?"
;;           "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 5 0]
;;   :total ["SELECT COUNT(*) AS count 
;;           FROM Person AS res 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE 
;;           INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE 
;;           INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE 
;;           WHERE (CAST(fname AS text) LIKE ?) 
;;               AND (CAST(lname AS text) LIKE ?) 
;;               AND (CAST(gender AS text) = ?)"
;;           "name" "given" "family" "gender" "%john%" "%doe%" "\"M\""]}
```

### Fns [wip]

### Queryp codes [wip]

| code             | example              | desc               | status      |
|------------------|----------------------|--------------------|-------------|
| :filters/text    | `name like '%john%'` | contains text      | implemented |
| :filters/keyword | `name = 'john'`      | match exact        | implemented |
| :filters/url     |                      |                    | planned     |
| :filters/number  |                      |                    | planned     |
| :filters/date    |                      |                    | planned     |
| :page/offset     | `OFFSET 0`           | page starting item | implemented |
| :page/limit      | `LIMIT 128`          | page size          | implemented |
|                  |                      |                    |             |
