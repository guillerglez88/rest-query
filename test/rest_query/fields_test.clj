(ns rest-query.fields-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [honey.sql :as hsql]
   [rest-query.fields :as sut]))

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
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS code ON TRUE")
            "code"]
           (-> (sut/all-by-type :Person)
               (sut/extract-prop :content {:name "code"} :code)
               (hsql/format))))))

(deftest extract-coll-test
  (testing "Can expose jsonb prop collection elements for filtering"
    (is (= [(str "SELECT res.* "
                  "FROM Person AS res "
                  "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS content_contacts ON TRUE "
                  "INNER JOIN JSONB_ARRAY_ELEMENTS(content_contacts) AS contact ON TRUE")
            "contacts"]
           (-> (sut/all-by-type :Person)
               (sut/extract-coll :content {:name "contacts" :collection true} :contact)
               (hsql/format)))))
  (testing "Can expose jsonb matching prop collection elements for filtering"
    (is (= [(str "SELECT res.* "
                "FROM Person AS res "
                "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS content_contacts ON TRUE "
                "INNER JOIN JSONB_ARRAY_ELEMENTS(content_contacts) AS contact ON (contact @> ?)")
            "contacts"
            {:code "code"}]
           (-> (sut/all-by-type :Person)
               (sut/extract-coll :content
                                 {:name "contacts"
                                  :collection true
                                  :filter {:code "code"}}
                                 :contact)
               (hsql/format))))))

(deftest extract-path-test
  (testing "Can access deep jsonb property"
    (is (= [(str "SELECT res.* "
                 "FROM Person AS res "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, ?) AS content_contacts ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(content_contacts) AS content_contacts_elem ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_contacts_elem, ?) AS contact_value ON TRUE")
            "contacts" "value"]
           (-> (sut/all-by-type :Person)
               (sut/extract-path :content
                                 [{:name "contacts" :collection true}
                                  {:name "value"}]
                                 :contact-value)
               (hsql/format))))))
