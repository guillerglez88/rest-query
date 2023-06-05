(ns rest-query.core-test
  (:require
   [clojure.test :as t :refer [deftest testing is]]
   [rest-query.core :as sut]
   [honey.sql :as hsql]))

(def queryps
  [{:name :fname, :code :filters/text, :path [{:field "resource"} {:field "name"} {:field "given", :coll true}]}
   {:name :lname, :code :filters/text, :path [{:field "resource"} {:field "name"} {:field "family"}]}
   {:name :gender, :code :filters/keyword, :path [{:field "resource"} {:field "gender"}]},
   {:name :age, :code :filters/number, :path [{:field "resource"} {:field "age"}]},
   {:name :_sort :code :page/sort :default "created"}
   {:name :_offset :code :page/offset :default 0}
   {:name :_limit, :code :page/limit, :default 128}])

(deftest make-query-test
  (testing "Can converst url-map into a query map"
    (is (= {:from :Person
            :hash "5bb4a09cc55aed5e6c03c47c4a7a5fa30280b58b80a2c6ac8775e5fddb35b34d"
            :page [(str "SELECT res.* "
                        "FROM Person AS res "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                        "WHERE (CAST(fname AS TEXT) LIKE ?) "
                          "AND (CAST(lname AS TEXT) LIKE ?) "
                          "AND (CAST(gender AS TEXT) = ?) "
                        "ORDER BY created ASC "
                        "LIMIT ? OFFSET ?")
                   "%john%" "%doe%" "\"M\"" 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS res "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                         "WHERE (CAST(fname AS TEXT) LIKE ?) "
                           "AND (CAST(lname AS TEXT) LIKE ?) "
                           "AND (CAST(gender AS TEXT) = ?) "
                        "ORDER BY created ASC")
                    "%john%" "%doe%" "\"M\""]}
           (sut/make-query {:from :Person
                            :params {"fname"   "john"
                                     "lname"   "doe"
                                     "gender"  "M"
                                     "_offset" 0
                                     "_limit"  5}}
                           queryps)))))

(deftest url->query-test
  (testing "Can converst url into a query map"
    (is (= {:from :Person
            :hash "bf6371020f87f0005e98b58b7d5387c5a85a6eccf29e46bfbfc5d5be2806b062"
            :page [(str "SELECT res.* "
                        "FROM Person AS res "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE "
                        "WHERE (CAST(fname AS TEXT) LIKE ?) "
                          "AND (CAST(lname AS TEXT) LIKE ?) "
                          "AND (CAST(gender AS TEXT) = ?) "
                          "AND (CAST(age AS DECIMAL) = ?) "
                        "ORDER BY created DESC "
                        "LIMIT ? OFFSET ?")
                   "%john%" "%doe%" "\"M\"" 35M 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS res "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, 'age') AS age ON TRUE "
                         "WHERE (CAST(fname AS TEXT) LIKE ?) "
                           "AND (CAST(lname AS TEXT) LIKE ?) "
                           "AND (CAST(gender AS TEXT) = ?) "
                           "AND (CAST(age AS DECIMAL) = ?) "
                        "ORDER BY created DESC")
                    "%john%" "%doe%" "\"M\"" 35M]}
           (sut/url->query "/Person?fname=john&lname=doe&gender=M&age=35&_sort=desc:created&_offset=0&_limit=5" queryps)))))

(deftest make-sql-map-test
  (testing "Can build coplex query with multiple filters"
    (let [params {"fname"   "john"
                  "lname"   "doe"
                  "gender"  "M"
                  "_offset" 5
                  "_limit"  20}]
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                   "WHERE (CAST(fname AS TEXT) LIKE ?) "
                     "AND (CAST(lname AS TEXT) LIKE ?) "
                     "AND (CAST(gender AS TEXT) = ?) "
                   "ORDER BY created ASC "
                   "LIMIT ? OFFSET ?")
              "%john%" "%doe%" "\"M\"" 20 5]
             (-> (sut/make-sql-map {:from :Person, :params params} queryps)
                 (hsql/format))))
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'name') AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'given') AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, 'family') AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, 'gender') AS gender ON TRUE "
                   "WHERE (CAST(fname AS TEXT) LIKE ?) "
                     "AND (CAST(lname AS TEXT) LIKE ?) "
                     "AND (CAST(gender AS TEXT) = ?) "
                   "ORDER BY created ASC "
                   "LIMIT ? OFFSET ?")
              "%john%" "%doe%" "\"M\"" 128 0]
             (-> (sut/make-sql-map {:from :Person, :params (select-keys params ["fname" "lname" "gender"])} queryps)
                 (hsql/format)))))))
