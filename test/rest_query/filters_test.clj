(ns rest-query.filters-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [honey.sql :as hsql]
   [rest-query.fields :as fields]
   [rest-query.filters :as sut]))

(deftest contains-text-test
  (testing "Can filter for occurrence of term on text field"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS name ON TRUE "
                 "WHERE CAST(name AS text) LIKE ?")
            "name" "%john%"]
           (-> (fields/all-by-type :Person)
               (fields/extract-path :content [{:name "name"}] :name)
               (sut/contains-text :name "john")
               (hsql/format))))))

(deftest match-exact-test
  (testing "Can filter for exact matching of term with text field"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS name ON TRUE "
                 "WHERE CAST(name AS text) = ?")
            "name" "\"John\""]
           (-> (fields/all-by-type :Person)
               (fields/extract-path :content [{:name "name"}] :name)
               (sut/match-exact :name "John")
               (hsql/format))))))

(deftest paginate-test
  (testing "Can paginate"
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "LIMIT")
            128]
           (-> (fields/all-by-type :Resource)
               (sut/page-size 128)
               (hsql/format))))))

(deftest paginate-test
  (testing "Can paginate"
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "OFFSET")
            10]
           (-> (fields/all-by-type :Resource)
               (sut/page-start 10)
               (hsql/format))))))

(deftest paginate-test
  (testing "Can paginate"
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "LIMIT ? OFFSET ?")
            128, 10]
           (-> (fields/all-by-type :Resource)
               (sut/page 10 128)
               (hsql/format))))))

(deftest total-test
  (testing "Can calc total query items"
    (is (= [(str "SELECT COUNT(*) AS count "
                 "FROM Resource AS res")]
           (-> (fields/all-by-type :Resource)
               (sut/total)
               (hsql/format))))))
