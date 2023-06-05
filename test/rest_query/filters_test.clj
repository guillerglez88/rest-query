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
                 "WHERE CAST(name AS TEXT) LIKE ?")
            "name" "%john%"]
           (-> (fields/all-by-type :Person)
               (fields/extract-path [{:field "content"} {:field "name"}] :name)
               (sut/contains-text :name "john")
               (hsql/format))))))

(deftest match-exact-test
  (testing "Can filter for exact matching of term with text field"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS name ON TRUE "
                 "WHERE CAST(name AS TEXT) = ?")
            "name" "\"John\""]
           (-> (fields/all-by-type :Person)
               (fields/extract-path [{:field "content"} {:field "name"}] :name)
               (sut/match-exact :name "John")
               (hsql/format))))))

(deftest number-test
  (testing "Can filter by number equals"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS age ON TRUE "
                 "WHERE CAST(age AS DECIMAL) = ?")
            "age" 35]
           (-> (fields/all-by-type :Person)
               (fields/extract-path [{:field "content"} {:field "age"}] :age)
               (sut/number :age 35 :op/eq)
               (hsql/format)))))
  (testing "Can filter by number <= n"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS age ON TRUE "
                 "WHERE CAST(age AS DECIMAL) <= ?")
            "age" 2]
           (-> (fields/all-by-type :Person)
               (fields/extract-path [{:field "content"} {:field "age"}] :age)
               (sut/number :age 2 :op/le)
               (hsql/format))))))

(deftest page-size-test
  (testing "Can set page size"
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "LIMIT ?")
            128]
           (-> (fields/all-by-type :Resource)
               (sut/page-size 128)
               (hsql/format))))))

(deftest page-start-test
  (testing "Can set page start"
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "OFFSET ?")
            10]
           (-> (fields/all-by-type :Resource)
               (sut/page-start 10)
               (hsql/format))))))

(deftest page-sort-test
  (testing "Can sort results"
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "ORDER BY created DESC")]
           (-> (fields/all-by-type :Resource)
               (sut/page-sort "created" :op/desc)
               (hsql/format))))
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "ORDER BY created ASC")]
           (-> (fields/all-by-type :Resource)
               (sut/page-sort "created" :op/asc)
               (hsql/format))))
    (is (= [(str "SELECT res.* "
                 "FROM Resource AS res "
                 "ORDER BY created ASC")]
           (-> (fields/all-by-type :Resource)
               (sut/page-sort "created" nil)
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
