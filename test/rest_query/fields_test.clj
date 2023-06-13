(ns rest-query.fields-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [honey.sql :as hsql]
   [rest-query.fields :as sut]
   [rest-query.util :as util]))

(deftest contains-alias?-test
  (testing "Can checkin if sql-map already contains aleas"
    (is (= true
           (-> (sut/all-by-type :Person :p)
               (sut/extract-prop :content {:field "code", :alias :code})
               (sut/contains-alias? :code))))
    (is (= false
           (-> (sut/all-by-type :Person :p)
               (sut/extract-prop :content {:field "code", :alias :code})
               (sut/contains-alias? :foo))))))

(deftest all-by-type-test
  (testing "Can make base sql map to filter results on"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p")]
           (-> (sut/all-by-type :Person :p)
               (hsql/format))))))

(deftest extract-prop-test
  (testing "Can expose jsonb prop for filtering"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(p.content, 'code') AS code ON TRUE")]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-prop :p.content {:field "code", :alias :code})
               (hsql/format))))))

(deftest extract-coll-test
  (testing "Can expose jsonb prop collection elements for filtering"
    (is (= [(str "SELECT p.* "
                  "FROM Person AS p "
                  "INNER JOIN JSONB_ARRAY_ELEMENTS(p_content_contacts) AS contact ON TRUE")]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-coll :p_content_contacts {:field "contacts", :coll true, :alias :contact})
               (hsql/format)))))
  (testing "Can expose jsonb matching prop collection elements for filtering"
    (is (= [(str "SELECT p.* "
                "FROM Person AS p "
                "INNER JOIN JSONB_ARRAY_ELEMENTS(p_content_contacts) AS contact ON (contact @> ?)")
            {:code "code"}]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-coll :p_content_contacts {:field "contacts", :coll true, :filter {:code "code"} :alias :contact})
               (hsql/format))))))

(deftest link-entity-test
  (testing "Can include linked external entity for filtering"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(p.content, 'org') AS p_content_org ON TRUE "
                 "INNER JOIN Organization AS p_content_org_entity ON CONCAT('/Organization/', p_content_org_entity.id) = CAST(p_content_org AS TEXT)")]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-prop :p.content {:field "org", :alias :p_content_org})
               (sut/link-entity :p_content_org {:field "org", :link "/Organization/id", :alias :p_content_org_entity})
               (hsql/format))))))

(deftest expand-path-test
  (testing "Can access deep jsonb property"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(p.content, 'contacts') AS p_content_contacts ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(p_content_contacts) AS p_content_contacts_elem ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(p_content_contacts_elem, 'value') AS contact_value ON TRUE")]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-path (util/expand-path [{:field "p.content"} {:field "contacts", :coll true} {:field "value", :alias :contact_value}]))
               (hsql/format)))))
  (testing "Can access deep jsonb property with shared path elem"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(p.content, 'name') AS p_content_name ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(p_content_name, 'given') AS fname ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(p_content_name, 'family') AS lname ON TRUE")]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-path (util/expand-path [{:field "p.content"} {:field "name"} {:field "given", :coll true, :alias :fname}]))
               (sut/extract-path (util/expand-path [{:field "p.content"} {:field "name"} {:field "family", :alias :lname}]))
               (hsql/format)))))
  (testing "Repeated props are not dupplicated"
    (is (= [(str "SELECT p.* "
                 "FROM Person AS p "
                 "INNER JOIN JSONB_EXTRACT_PATH(content, 'name') AS content_name ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_name, 'given') AS fname ON TRUE "
                 "INNER JOIN JSONB_ARRAY_ELEMENTS(fname) AS fname_elem ON TRUE "
                 "INNER JOIN JSONB_EXTRACT_PATH(content_name, 'family') AS lname ON TRUE")]
           (-> (sut/all-by-type :Person :p)
               (sut/extract-path (util/expand-path [{:field "content"} {:field "name"} {:field "given", :coll true, :alias :fname}]))
               (sut/extract-path (util/expand-path [{:field "content"} {:field "name"} {:field "family", :alias :lname}]))
               (sut/extract-path (util/expand-path [{:field "content"} {:field "name"} {:field "family", :alias :lname}]))
               (hsql/format))))))
