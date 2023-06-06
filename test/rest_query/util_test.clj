(ns rest-query.util-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [rest-query.util :as sut]))

(deftest uri->map
  (testing "Can convert uri into a map with the data needed for query"
    (is (= {:from :Person
            :params {"fname" "john"
                     "lname" "doe"
                     "gender" "M"
                     "_offset" "0"
                     "_limit" "5"}}
           (sut/url->map "/Person?fname=john&lname=doe&gender=M&_offset=0&_limit=5")))))

(deftest get-param-test
  (testing "Can extract param value and operation"
    (is (= ["35"]
           (sut/get-param {"age" "35"} :age)))
    (is (= ["21" :op/gt]
           (sut/get-param {"age" "gt:21"} :age)))
    (is (= ["2" :op/lt]
           (sut/get-param {"age" "lt:2"} :age)))
    (is (= ["8:00-17:30" :op/bw]
           (sut/get-param {"time" "bw:8:00-17:30"} :time)))
    (is (= ["8:00-17:30"]
           (sut/get-param {"time" "esc:8:00-17:30"} :time)))
    (is (= [nil]
           (sut/get-param {"foo" "bar"} :baz)))
    (is (= ["128"]
           (sut/get-param {"_limit" 128} :_limit)))
    (is (= ["128"]
           (sut/get-param {"foo" "bar"} :_limit {:default 128})))
    (is (= [35]
           (sut/get-param {"age" "35"} :age {:parser #(Integer/parseInt %)})))
    (is (= [128]
           (sut/get-param {"foo" "bar"} :_limit {:default 128
                                                 :parser #(Integer/parseInt %)})))))

(deftest calc-hash-test
  (testing "Can calc digest from string"
    (is (= "24efa410601485f435ca2a655ee5bc14707f030c923ad7be9329eb45c07fc40c"
           (sut/calc-hash "SELECT id, resource, created, modified FROM Resource LIMIT ? OFFSET ?")))))

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
           (sut/make-alias :resource "path" nil)))
    (is (= :person
           (sut/make-alias :Person)))
    (is (= :person_resource
           (sut/make-alias :Person "resource")))
    (is (= :p_content_contacts
           (sut/make-alias :p.content "contacts")))))

(deftest assign-alias-test
  (testing "Can assign alias to path elem"
    (is (= {:field "created", :alias :created}
           (sut/assign-alias nil {:field "created"})))
    (is (= {:field "p.created", :alias :p.created}
           (sut/assign-alias nil {:field "p.created"})))
    (is (= {:field "name", :alias :p_content_name}
           (sut/assign-alias :p.content {:field "name"})))
    (is (= {:field "contacts", :coll true, :alias :p_content_contacts_elem}
           (sut/assign-alias :p.content {:field "contacts", :coll true}))))
  (testing "Can doesn't override alias"
    (is (= {:field "created", :alias :c}
           (sut/assign-alias nil {:field "created", :alias :c})))))

(deftest assign-aliases-test
  (testing "Can assign alias to each element in the path"
    (is (= [{:field "p.resource", :alias :p.resource}
            {:field "name", :alias :p_resource_name}
            {:field "given", :coll true, :alias :p_resource_name_given_elem}]
           (sut/assign-aliasses [{:field "p.resource"}
                                 {:field "name"}
                                 {:field "given", :coll true}])))
    (is (= [{:field "p.resource", :alias :p.resource}
            {:field "name", :alias :p_resource_name}
            {:field "given", :coll true, :alias :first_name}]
           (sut/assign-aliasses [{:field "p.resource"}
                                 {:field "name"}
                                 {:field "given", :coll true, :alias :first_name}])))))
