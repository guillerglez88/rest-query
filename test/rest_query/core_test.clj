(ns rest-query.core-test
  (:require
   [clojure.test :as t :refer [deftest testing is]]
   [rest-query.core :as sut]
   [honey.sql :as hsql]))

(deftest make-sql-map-test
  (testing "Can build coplex query with multiple filters"
    (let [queryps [{:name :fname
                    :code :filters/text
                    :path [{:name "name"}
                           {:name "given", :collection true}]}
                   {:name :lname
                    :code :filters/text
                    :path [{:name "name"}
                           {:name "family"}]}
                   {:name :gender
                    :code :filters/keyword
                    :path [{:name "gender"}]}
                   {:name :_offset
                    :code :page/offset
                    :value 0}
                   {:name :_limit
                    :code :page/limit
                    :value 128}]
          params {"fname"   "john"
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
                   "WHERE (CAST(fname AS text) LIKE ?) "
                   "AND (CAST(lname AS text) LIKE ?) "
                   "AND (CAST(gender AS text) = ?) LIMIT ? OFFSET ?")
              "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 20 5]
             (-> (sut/make-sql-map :Person :resource queryps params)
                 (hsql/format))))
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS resource_name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS resource_name_given ON TRUE "
                   "INNER JOIN JSONB_ARRAY_ELEMENTS(resource_name_given) AS fname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource_name, ?) AS lname ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                   "WHERE (CAST(fname AS text) LIKE ?) "
                   "AND (CAST(lname AS text) LIKE ?) "
                   "AND (CAST(gender AS text) = ?) LIMIT ? OFFSET ?")
              "name" "given" "family" "gender" "%john%" "%doe%" "\"M\"" 128 0]
             (-> (sut/make-sql-map :Person :resource queryps (select-keys params ["fname" "lname" "gender"]))
                 (hsql/format)))))))
