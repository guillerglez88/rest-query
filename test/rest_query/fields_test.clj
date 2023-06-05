(ns rest-query.fields-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [honey.sql :as hsql]
   [rest-query.fields :as sut]))

(deftest contains-alias?-test
  (testing "Can checkin if sql-map already contains aleas"
    (is (= true
           (-> (sut/all-by-type :Person)
               (sut/extract-prop :content {:name "code"} :code)
               (sut/contains-alias? :code))))
    (is (= false
           (-> (sut/all-by-type :Person)
               (sut/extract-prop :content {:name "code"} :code)
               (sut/contains-alias? :foo))))))

(deftest make-alias-test
  (testing "Can make pg-sql compliant field name"
    (is (= :path
           (sut/make-alias "path")))
    (is (= :path_to_field
           (sut/make-alias "path-to-field")))
    (is (= :path_to_field
           (sut/make-alias "path-to-field  ")))
    (is (= :resource_code
           (sut/make-alias :resource "code")))
    (is (= :resource_path_elem
           (sut/make-alias :resource "path" "elem ")))
    (is (= :resource_path_elem
           (sut/make-alias :resource "path" :elem)))
    (is (= :resource_path
           (sut/make-alias :resource "path" nil)))))

(deftest all-by-type-test
  (testing "Can make base sql map to filter results on"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res")]
           (-> (sut/all-by-type :Person)
               (hsql/format))))))

(deftest extract-prop-test
  (testing "Can expose jsonb prop for filtering"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'code') AS code ON TRUE")]
           (-> (sut/all-by-type :Person)
               (sut/extract-prop :content {:field "code"} :code)
               (hsql/format))))))

(deftest extract-coll-test
  (testing "Can expose jsonb prop collection elements for filtering"
    (is (= [(str "SELECT res.* "
                  "FROM Person AS res "
                  "INNER JOIN JSONB_EXTRACT_PATH(content, 'contacts') AS content_contacts ON TRUE "
                  "INNER JOIN JSONB_ARRAY_ELEMENTS(content_contacts) AS contact ON TRUE")]
           (-> (sut/all-by-type :Person)
               (sut/extract-coll :content {:field "contacts" :coll true} :contact)
               (hsql/format)))))
  (testing "Can expose jsonb matching prop collection elements for filtering"
    (is (= [(str "SELECT res.* "
                "FROM Person AS res "
                "INNER JOIN JSONB_EXTRACT_PATH(content, 'contacts') AS content_contacts ON TRUE "
                "INNER JOIN JSONB_ARRAY_ELEMENTS(content_contacts) AS contact ON (contact @> ?)")
            {:code "code"}]
           (-> (sut/all-by-type :Person)
               (sut/extract-coll :content
                                 {:field "contacts"
                                  :coll true
                                  :filter {:code "code"}}
                                 :contact)
               (hsql/format))))))

(deftest extract-path-test
  (testing "Can access deep jsonb property"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'contacts') AS content_contacts ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(content_contacts) AS content_contacts_elem ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_contacts_elem, 'value') AS contact_value ON TRUE")]
           (-> (sut/all-by-type :Person)
               (sut/extract-path [{:field "content"}
                                  {:field "contacts" :coll true}
                                  {:field "value"}]
                                 :contact-value)
               (hsql/format)))))
  (testing "Can access deep jsonb property with shared path elem"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'name') AS content_name ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_name, 'given') AS content_name_given ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(content_name_given) AS fname ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_name, 'family') AS lname ON TRUE")]
           (-> (sut/all-by-type :Person)
               (sut/extract-path [{:field "content"} {:field "name"} {:field "given", :coll true}] :fname)
               (sut/extract-path [{:field "content"} {:field "name"} {:field "family"}] :lname)
               (hsql/format)))))
  (testing "Repeated props are not dupplicated"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'name') AS content_name ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_name, 'given') AS content_name_given ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(content_name_given) AS fname ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_name, 'family') AS lname ON TRUE")]
           (-> (sut/all-by-type :Person)
               (sut/extract-path [{:field "content"} {:field "name"} {:field "given", :coll true}] :fname)
               (sut/extract-path [{:field "content"} {:field "name"} {:field "family"}] :lname)
               (sut/extract-path [{:field "content"} {:field "name"} {:field "family"}] :lname)
               (hsql/format))))))
