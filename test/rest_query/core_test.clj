(ns rest-query.core-test
  (:require
   [clojure.test :as t :refer [deftest testing is]]
   [rest-query.core :as sut]
   [honey.sql :as hsql]))

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

   {:code :filters/date
    :name "_created"
    :path [{:field "created"}]
    :default []}

   {:code :page/sort
    :name "sort"
    :default "_created"}

   {:code :page/offset
    :name "page-start"
    :default 0}

   {:code :page/limit
    :name "page-size"
    :default 128}])

(deftest make-query-test
  (testing "Can converst url-map into a query map"
    (is (= {:from :Person
            :hash "c6736582d38fd8268ac7b4e894d65d74f1ab758a810c73b79bcd54de5de773a6"
            :page [(str "SELECT person.* "
                        "FROM Person AS person "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                        "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
                          "AND (CAST(lname AS TEXT) LIKE ?) "
                          "AND (CAST(gender AS TEXT) = ?) "
                        "ORDER BY created ASC "
                        "LIMIT ? OFFSET ?")
                   "%john%" "%doe%" "\"M\"" 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS person "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                         "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
                           "AND (CAST(lname AS TEXT) LIKE ?) "
                           "AND (CAST(gender AS TEXT) = ?)")
                    "%john%" "%doe%" "\"M\""]}
           (sut/make-query {:from :Person
                            :params {"fname"   "john"
                                     "lname"   "doe"
                                     "gender"  "M"
                                     "page-start" 0
                                     "page-size"  5}}
                           queryps)))))

(deftest url->query-test
  (testing "Can converst url into a query map"
    (is (= {:from :Person
            :hash "6879c20ddf8a6e87921efd122d72e2ad66e6b24085ef2252d49e4c8186f91081"
            :page [(str "SELECT person.* "
                        "FROM Person AS person "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS org ON TRUE "
                        "INNER JOIN Organization AS org_entity ON CONCAT('/Organization/', org_entity.id) = CAST(org AS TEXT) "
                        "INNER JOIN JSONB_EXTRACT_PATH(org_entity.resource, 'name') AS org_name ON TRUE "
                        "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
                          "AND (CAST(lname AS TEXT) LIKE ?) "
                          "AND (CAST(gender AS TEXT) = ?) "
                          "AND (CAST(age AS DECIMAL) = ?) "
                          "AND (CAST(org_name AS TEXT) LIKE ?) "
                        "ORDER BY created DESC "
                        "LIMIT ? OFFSET ?")
                   "%john%" "%doe%" "\"M\"" 35M "%MyOrg%" 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS person "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS org ON TRUE "
                         "INNER JOIN Organization AS org_entity ON CONCAT('/Organization/', org_entity.id) = CAST(org AS TEXT) "
                         "INNER JOIN JSONB_EXTRACT_PATH(org_entity.resource, 'name') AS org_name ON TRUE "
                         "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
                           "AND (CAST(lname AS TEXT) LIKE ?) "
                           "AND (CAST(gender AS TEXT) = ?) "
                           "AND (CAST(age AS DECIMAL) = ?) "
                           "AND (CAST(org_name AS TEXT) LIKE ?)")
                    "%john%" "%doe%" "\"M\"" 35M "%MyOrg%"]}
           (sut/url->query "/Person?fname=john&lname=doe&gender=M&age=35&org-name=MyOrg&sort:desc=_created&page-start=0&page-size=5" queryps)))))

(deftest make-sql-map-test
  (testing "Can build coplex query with multiple filters"
    (let [params {"fname"   "john"
                  "lname"   "doe"
                  "gender"  "M"
                  "page-start" 5
                  "page-size"  20}]
      (is (= [(str "SELECT person.* "
                   "FROM Person AS person "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                   "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
                     "AND (CAST(lname AS TEXT) LIKE ?) "
                     "AND (CAST(gender AS TEXT) = ?) "
                   "ORDER BY created ASC "
                   "LIMIT ? OFFSET ?")
              "%john%" "%doe%" "\"M\"" 20 5]
             (-> (sut/make-sql-map {:from :Person, :params params} queryps)
                 (hsql/format))))
      (is (= [(str "SELECT person.* "
                   "FROM Person AS person "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS fname ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                   "WHERE (CAST(fname_elem AS TEXT) LIKE ?) "
                     "AND (CAST(lname AS TEXT) LIKE ?) "
                     "AND (CAST(gender AS TEXT) = ?) "
                   "ORDER BY created ASC "
                   "LIMIT ? OFFSET ?")
              "%john%" "%doe%" "\"M\"" 128 0]
             (-> (sut/make-sql-map {:from :Person, :params (select-keys params ["fname" "lname" "gender"])} queryps)
                 (hsql/format)))))))
