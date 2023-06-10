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
           (sut/url->map "/Person?fname=john&lname=doe&gender=M&_offset=0&_limit=5")))
    (is (= {:from :Person
            :params {"fname" "john"
                     "lname" "doe"
                     "age:ge" "21"
                     "gender" "M"
                     "_offset" "0"
                     "_limit" "5"}}
           (sut/url->map "/Person?fname=john&lname=doe&age:ge=21&gender=M&_offset=0&_limit=5")))
    (is (= {:from :Person
            :params {"name" "john"
                     "gender" ["M" "F"]}}
           (sut/url->map "/Person?name=john&gender=M&gender=F")))
    (is (= {:from :Person
            :params {"name" "john"
                     "gender" "M,F"}}
           (sut/url->map "/Person?name=john&gender=M,F")))))

(deftest parse-param-key-test
  (testing "Can parse param key to extract name and operation"
    (is (= ["name"]
           (sut/parse-param-key "name")))
    (is (= ["age" :op/gt]
           (sut/parse-param-key "age:gt")))))

(deftest process-params-test
  (testing "Can process params map to extract operations"
    (is (= {"age" ["21" :op/gt]
            "name" ["john" nil]
            "gender" ["F" "M" nil]}
           (sut/process-params {"age:gt" "21"
                                "name" "john"
                                "gender" ["F" "M"]})))))

(deftest get-param-test
  (testing "Can extract param value and operation"
    (is (= ["35" nil]
           (sut/get-param {"age" ["35" nil]} {:name :age})))
    (is (= ["21" :op/gt]
           (sut/get-param {"age" ["21" :op/gt]} {:name :age})))
    (is (= ["2" :op/lt]
           (sut/get-param {"age" ["2" :op/lt]} {:name :age})))
    (is (= ["8:00-17:30" :op/bw]
           (sut/get-param {"time" ["8:00-17:30" :op/bw]} {:name :time})))
    (is (= ["8:00-17:30" nil]
           (sut/get-param {"time" ["8:00-17:30" nil]} {:name :time})))
    (is (= ["" nil]
           (sut/get-param {"foo" ["bar" nil]} {:name :baz})))
    (is (= ["128" nil]
           (sut/get-param {"_limit" ["128" nil]} {:name :_limit})))
    (is (= ["128" nil]
           (sut/get-param {"foo" ["bar" nil]} {:name :_limit, :default 128})))
    (is (= [35 :op/gt]
           (sut/get-param {"age" ["35" :op/gt]} {:name :age} #(Integer/parseInt %))))
    (is (= [128 nil]
           (sut/get-param {"foo" ["bar" nil]} {:name :_limit, :default 128} #(Integer/parseInt %))))))

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
           (sut/make-alias :p.content "contacts")))
    (is (= :org_name
           (sut/make-alias :org-name)))
    (is (= :org_entity_resource_name
           (sut/make-alias "org_entity_resource." "name")))))

(deftest assign-alias-test
  (testing "Can assign alias to path elem"
    (is (= {:field "created", :alias :created, :root true}
           (sut/assign-alias {:field "created", :root true} nil)))
    (is (= {:field "name", :alias :content_name, :root false}
           (sut/assign-alias {:field "name", :root false}
                             {:field "content" :alias :content, :root true})))
    (is (= {:field "contacts", :coll true, :alias :contacts_elem, :root false}
           (sut/assign-alias {:field "contacts", :coll true, :alias :contacts, :root false}
                             {:field "contacts", :alias :contacts, :root false})))
    (is (= {:field "org", :link "/Organization/id", :alias :content_org_entity, :root false}
           (sut/assign-alias {:field "org", :link "/Organization/id", :alias :content_org, :root false}
                             {:field "org", :alias :content_org, :root false}))))
  (testing "Assign doesn't override alias"
    (is (= {:field "created", :alias :c}
           (sut/assign-alias {:field "created", :alias :c} nil))))
  (testing "String alias is converted into keyword"
    (is (= {:field "created", :alias :creation_date}
           (sut/assign-alias {:field "created", :alias "creation-date"} nil)))))

(deftest expand-elem-test
  (testing "Can expand path element"
    (is (= [{:field "content", :root true}]
           (sut/expand-elem {:field "content"} nil)))
    (is (= [{:field "name", :root false}]
           (sut/expand-elem {:field "name"} {:field "content"})))
    (is (= [{:field "contacts", :root false}
            {:field "contacts", :coll true, :root false}]
           (sut/expand-elem {:field "contacts", :coll true} {:field "content"})))
    (is (= [{:field "org", :root false}
            {:field "org", :link "/Organization/id", :root false}]
           (sut/expand-elem {:field "org", :link "/Organization/id"} {:field "content"})))
    (is (= [{:field "content", :root true}]
           (sut/expand-elem {:field "content"} {:field "org", :link "/Organization/id"})))))

(deftest prepare-path-test
  (testing "Can pre-process path"
    (is (= [{:field "resource", :root true, :alias :resource}
            {:field "name", :root false, :alias :resource_name}
            {:field "given", :root false, :alias :resource_name_given}
            {:field "given", :coll true, :root false, :alias :resource_name_given_elem}]
           (sut/prepare-path [{:field "resource"}
                              {:field "name"}
                              {:field "given", :coll true}])))
    (is (= [{:field "resource", :alias :resource, :root true}
            {:field "name", :alias :resource_name, :root false}
            {:field "given", :alias :first_name, :root false}
            {:field "given", :coll true, :alias :first_name_elem, :root false}]
           (sut/prepare-path [{:field "resource"}
                              {:field "name"}
                              {:field "given", :coll true, :alias :first_name}])))
    (is (= [{:field "resource", :alias :resource, :root true}
            {:field "org", :alias :resource_org, :root false}
            {:field "org", :link "/Organization/id", :alias :resource_org_entity, :root false}
            {:field "resource", :alias :resource_org_entity.resource, :root true}
            {:field "name", :alias :resource_org_entity_resource_name, :root false}]
           (sut/prepare-path [{:field "resource"}
                              {:field "org", :link "/Organization/id"}
                              {:field "resource"}
                              {:field "name"}])))))
