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

(deftest make-query-test
  (testing "Can converst url-map into a query map"
    (is (= {:from :Person
            :hash "240fbc32ffe828a96da083434e2b812a847c7972267535300d926789514cded4"
            :page [(str "SELECT person.* "
                        "FROM Person AS person "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE "
                        "WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) "
                          "AND (CAST(resource_name_family AS TEXT) LIKE ?) "
                          "AND (CAST(resource_gender AS TEXT) = ?) "
                        "ORDER BY created ASC "
                        "LIMIT ? OFFSET ?")
                   "%john%" "%doe%" "\"M\"" 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS person "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE "
                         "WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) "
                           "AND (CAST(resource_name_family AS TEXT) LIKE ?) "
                           "AND (CAST(resource_gender AS TEXT) = ?) "
                         "ORDER BY created ASC")
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
            :hash "0fbe0b80f2e3c87439babf56561dbf231f2a1f6b56f92a37b71d484e8b4539fa"
            :page [(str "SELECT person.* "
                        "FROM Person AS person "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS resource_age ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS resource_organization ON TRUE "
                        "INNER JOIN Organization AS resource_organization_entity ON CONCAT('/Organization/', resource_organization_entity.id) = CAST(resource_organization AS TEXT) "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_organization_entity.resource, 'name') AS resource_organization_entity_resource_name ON TRUE "
                        "WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) "
                          "AND (CAST(resource_name_family AS TEXT) LIKE ?) "
                          "AND (CAST(resource_gender AS TEXT) = ?) "
                          "AND (CAST(resource_age AS DECIMAL) = ?) "
                          "AND (CAST(resource_organization_entity_resource_name AS TEXT) LIKE ?) "
                        "ORDER BY created DESC "
                        "LIMIT ? OFFSET ?")
                   "%john%" "%doe%" "\"M\"" 35M "%MyOrg%" 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS person "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS resource_age ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'organization') AS resource_organization ON TRUE "
                         "INNER JOIN Organization AS resource_organization_entity ON CONCAT('/Organization/', resource_organization_entity.id) = CAST(resource_organization AS TEXT) "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_organization_entity.resource, 'name') AS resource_organization_entity_resource_name ON TRUE "
                         "WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) "
                           "AND (CAST(resource_name_family AS TEXT) LIKE ?) "
                           "AND (CAST(resource_gender AS TEXT) = ?) "
                           "AND (CAST(resource_age AS DECIMAL) = ?) "
                           "AND (CAST(resource_organization_entity_resource_name AS TEXT) LIKE ?) "
                         "ORDER BY created DESC")
                    "%john%" "%doe%" "\"M\"" 35M "%MyOrg%"]}
           (sut/url->query "/Person?fname=john&lname=doe&gender=M&age=35&org-name=MyOrg&sort:desc=created&page-start=0&page-size=5" queryps)))))

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
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE "
                   "WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) "
                     "AND (CAST(resource_name_family AS TEXT) LIKE ?) "
                     "AND (CAST(resource_gender AS TEXT) = ?) "
                   "ORDER BY created ASC "
                   "LIMIT ? OFFSET ?")
              "%john%" "%doe%" "\"M\"" 20 5]
             (-> (sut/make-sql-map {:from :Person, :params params} queryps)
                 (hsql/format))))
      (is (= [(str "SELECT person.* "
                   "FROM Person AS person "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS resource_name_given_elem ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS resource_name_family ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS resource_gender ON TRUE "
                   "WHERE (CAST(resource_name_given_elem AS TEXT) LIKE ?) "
                     "AND (CAST(resource_name_family AS TEXT) LIKE ?) "
                     "AND (CAST(resource_gender AS TEXT) = ?) "
                   "ORDER BY created ASC "
                   "LIMIT ? OFFSET ?")
              "%john%" "%doe%" "\"M\"" 128 0]
             (-> (sut/make-sql-map {:from :Person, :params (select-keys params ["fname" "lname" "gender"])} queryps)
                 (hsql/format)))))))
