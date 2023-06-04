(ns rest-query.core-test
  (:require
   [clojure.test :as t :refer [deftest testing is]]
   [rest-query.core :as sut]
   [honey.sql :as hsql]))

(def queryps
  [{:name :fname, :code :filters/text, :path [{:prop "name"} {:prop "given", :coll true}]}
   {:name :lname, :code :filters/text, :path [{:prop "name"} {:prop "family"}]}
   {:name :gender, :code :filters/keyword, :path [{:prop "gender"}]},
   {:name :age, :code :filters/number, :path [{:prop "age"}]},
   {:name :_offset :code :page/offset :default 0}
   {:name :_limit, :code :page/limit, :default 128}])

(deftest make-query-test
  (testing "Can converst url-map into a query map"
    (is (= {:from :Person
            :page [(str "SELECT res.* "
                        "FROM Person AS res "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                        "WHERE (CAST(fname AS TEXT) LIKE ?) "
                          "AND (CAST(lname AS TEXT) LIKE ?) "
                          "AND (CAST(gender AS TEXT) = ?) "
                        "LIMIT ? OFFSET ?")
                   "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS res "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                         "WHERE (CAST(fname AS TEXT) LIKE ?) "
                           "AND (CAST(lname AS TEXT) LIKE ?) "
                           "AND (CAST(gender AS TEXT) = ?)")
                    "name" "given" "family" "gender" "%john%" "%doe%" "\"M\""]}
           (sut/make-query {:from :Person
                            :params {"fname"   "john"
                                     "lname"   "doe"
                                     "gender"  "M"
                                     "_offset" 0
                                     "_limit"  5}}
                           :resource
                           queryps)))))

(deftest url->query-test
  (testing "Can converst url into a query map"
    (is (= {:from :Person
            :page [(str "SELECT res.* "
                        "FROM Person AS res "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                        "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                        "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS age ON TRUE "
                        "WHERE (CAST(fname AS TEXT) LIKE ?) "
                          "AND (CAST(lname AS TEXT) LIKE ?) "
                          "AND (CAST(gender AS TEXT) = ?) "
                          "AND (CAST(age AS DECIMAL) = ?) "
                        "LIMIT ? OFFSET ?")
                   "name" "given" "family" "gender" "age" "%john%" "%doe%" "\"M\"" 35M 5 0]
            :total [(str "SELECT COUNT(*) AS count "
                         "FROM Person AS res "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                         "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                         "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS age ON TRUE "
                         "WHERE (CAST(fname AS TEXT) LIKE ?) "
                           "AND (CAST(lname AS TEXT) LIKE ?) "
                           "AND (CAST(gender AS TEXT) = ?) "
                           "AND (CAST(age AS DECIMAL) = ?)")
                    "name" "given" "family" "gender" "age" "%john%" "%doe%" "\"M\"" 35M]}
           (sut/url->query (str "/Person"
                                "?fname=john"
                                "&lname=doe"
                                "&gender=M"
                                "&age=35"
                                "&_offset=0"
                                "&_limit=5")
                           :resource
                           queryps)))))

(deftest make-sql-map-test
  (testing "Can build coplex query with multiple filters"
    (let [params {"fname"   "john"
                  "lname"   "doe"
                  "gender"  "M"
                  "_offset" 5
                  "_limit"  20}]
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                   "WHERE (CAST(fname AS TEXT) LIKE ?) "
                   "AND (CAST(lname AS TEXT) LIKE ?) "
                   "AND (CAST(gender AS TEXT) = ?) LIMIT ? OFFSET ?")
              "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 20 5]
             (-> (sut/make-sql-map {:from :Person, :params params}
                                   :resource
                                   queryps)
                 (hsql/format))))
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                   "WHERE (CAST(fname AS TEXT) LIKE ?) "
                   "AND (CAST(lname AS TEXT) LIKE ?) "
                   "AND (CAST(gender AS TEXT) = ?) LIMIT ? OFFSET ?")
              "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 128 0]
             (-> (sut/make-sql-map {:from :Person, :params (select-keys params ["fname" "lname" "gender"])}
                                   :resource
                                   queryps)
                 (hsql/format)))))))
