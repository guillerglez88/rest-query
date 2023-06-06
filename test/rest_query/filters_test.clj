(ns rest-query.filters-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [honey.sql :as hsql]
   [rest-query.fields :as fields]
   [rest-query.filters :as sut]))

(deftest contains-text-test
  (testing "Can filter for occurrence of term on text field"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'name') AS name ON TRUE "
                 "WHERE CAST(name AS TEXT) LIKE ?")
            "%john%"]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content"} {:field "name"}] :name)
               (sut/contains-text :name "john")
               (hsql/format))))))

(deftest match-exact-test
  (testing "Can filter for exact matching of term with text field"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'name') AS name ON TRUE "
                 "WHERE CAST(name AS TEXT) = ?")
            "\"John\""]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content"} {:field "name"}] :name)
               (sut/match-exact :name "John")
               (hsql/format))))))

(deftest number-test
  (testing "Can filter by number equals"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'age') AS age ON TRUE "
                 "WHERE CAST(age AS DECIMAL) = ?")
            35]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content"} {:field "age"}] :age)
               (sut/number :age 35 :op/eq)
               (hsql/format)))))
  (testing "Can filter by number <= n"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'age') AS age ON TRUE "
                 "WHERE CAST(age AS DECIMAL) <= ?")
            2]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content"} {:field "age"}] :age)
               (sut/number :age 2 :op/le)
               (hsql/format))))))

(deftest page-size-test
  (testing "Can set page size"
    (is (= [(str "SELECT r.* "
                 "FROM Resource AS r "
                 "LIMIT ?")
            128]
           (-> (fields/all-by-type :Resource :r)
               (sut/page-size 128)
               (hsql/format))))))

(deftest page-start-test
  (testing "Can set page start"
    (is (= [(str "SELECT r.* "
                 "FROM Resource AS r "
                 "OFFSET ?")
            10]
           (-> (fields/all-by-type :Resource :r)
               (sut/page-start 10)
               (hsql/format))))))

(deftest page-sort-test
  (testing "Can sort results"
    (is (= [(str "SELECT r.* "
                 "FROM Resource AS r "
                 "ORDER BY created DESC")]
           (-> (fields/all-by-type :Resource :r)
               (sut/page-sort "created" :op/desc)
               (hsql/format))))
    (is (= [(str "SELECT r.* "
                 "FROM Resource AS r "
                 "ORDER BY created ASC")]
           (-> (fields/all-by-type :Resource :r)
               (sut/page-sort "created" :op/asc)
               (hsql/format))))
    (is (= [(str "SELECT r.* "
                 "FROM Resource AS r "
                 "ORDER BY created ASC")]
           (-> (fields/all-by-type :Resource :r)
               (sut/page-sort "created" nil)
               (hsql/format))))))

(deftest paginate-test
  (testing "Can paginate"
    (is (= [(str "SELECT r.* "
                 "FROM Resource AS r "
                 "LIMIT ? OFFSET ?")
            128, 10]
           (-> (fields/all-by-type :Resource :r)
               (sut/page 10 128)
               (hsql/format))))))

(deftest total-test
  (testing "Can calc total query items"
    (is (= [(str "SELECT COUNT(*) AS count "
                 "FROM Resource AS r")]
           (-> (fields/all-by-type :Resource :r)
               (sut/total)
               (hsql/format))))))
