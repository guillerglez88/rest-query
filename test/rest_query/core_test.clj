(ns rest-query.core-test
  (:require
   [clojure.test :as t :refer [deftest testing is]]
   [rest-query.core :as sut]
   [honey.sql :as hsql]))

(deftest make-query-test
  (testing "Can build coplex query with multiple filters"
    (let [queryps [{:name :name
                    :code sut/flt-text
                    :path [{:name "name"}]}
                   {:name :gender
                    :code sut/flt-keyword
                    :path [{:name "gender"}]}
                   {:name :_offset
                    :code sut/pag-offset
                    :value 0}
                   {:name :_limit
                    :code sut/pag-limit
                    :value 128}]
          params {"name"    "john"
                  "gender"  "M"
                  "_offset" 5
                  "_limit"  20}]
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                   "WHERE (CAST(name AS text) LIKE ?) "
                   "AND (CAST(gender AS text) = ?) "
                   "LIMIT ? OFFSET ?")
              "name" "gender" "%john%" "\"M\"" 20 5]
             (-> (sut/make-query :Person queryps params)
                 (hsql/format))))
      (is (= [(str "SELECT res.* "
                   "FROM Person AS res "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS name ON TRUE "
                   "INNER JOIN JSONB_EXTRACT_PATH(resource, ?) AS gender ON TRUE "
                   "WHERE (CAST(name AS text) LIKE ?) "
                   "AND (CAST(gender AS text) = ?) "
                   "LIMIT ? OFFSET ?")
              "name" "gender" "%john%" "\"M\"" 128 0]
             (-> (sut/make-query :Person queryps (select-keys params ["name" "gender"]))
                 (hsql/format)))))))
