# rest-query

Clojure library for translating REST url query-string into a database query

## Desc [wip]

Ability to convert raw URL and query-string params metadata into SQL query. By default, pg-sql is being targeted but the client code is provided with a hash unique for each specific query, not the parameter values, so the client code can optimize the generated query by mapping the query based on the provided hash. This also makes it possible, porting queries to a different database or even a different query language. Honey.sql is being used under the hood.

## Reference [wip]

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
