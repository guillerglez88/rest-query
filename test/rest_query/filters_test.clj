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
                 "WHERE (CAST(name AS TEXT) LIKE ?) "
                 "AND (CAST(desc AS TEXT) LIKE ?) "
                 "AND (CAST(desc AS TEXT) LIKE ?)")
            "%john%" "%lorem%" "%ipsum%"]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content"} {:field "name", :alias :name}])
               (first)
               (sut/contains-text :name ["john"])
               (sut/contains-text :desc ["lorem" "ipsum"])
               (hsql/format))))))

(deftest match-exact-test
  (testing "Can filter for exact matching of term with text field"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'status') AS status ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'gender') AS gender ON TRUE "
                 "WHERE (CAST(status AS TEXT) = ?) "
                 "AND (CAST(gender AS TEXT) IN (?, ?))")
            "\"active\"" "\"M\"" "\"F\""]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content", :alias :content} {:field "status", :alias :status}])
               (first)
               (fields/extract-path [{:field "content", :alias :content} {:field "gender", :alias :gender}])
               (first)
               (sut/match-exact :status ["active"])
               (sut/match-exact :gender ["M" "F"])
               (hsql/format))))))

(deftest number-test
  (testing "Can filter by number equals"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'age') AS age ON TRUE "
                 "WHERE CAST(age AS DECIMAL) = ?")
            35]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content", :alias :content} {:field "age", :alias :age}])
               (first)
               (sut/number :age [35] :op/eq)
               (hsql/format)))))
  (testing "Can filter by number <= n"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'age') AS age ON TRUE "
                 "WHERE CAST(age AS DECIMAL) <= ?")
            2]
           (-> (fields/all-by-type :Person :p)
               (fields/extract-path [{:field "content", :alias :content} {:field "age", :alias :age}])
               (first)
               (sut/number :age [2] :op/le)
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
