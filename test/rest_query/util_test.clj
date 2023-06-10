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

(deftest queryp->values
  (testing "Can map queryp into values vector"
    (is (= [:op/_nil]
           (sut/queryp->values {:code :filters/text})))
    (is (= [:op/_nil "foo"]
           (sut/queryp->values {:code :filters/text, :default "foo"})))
    (is (= [:op/_nil 5]
           (sut/queryp->values {:code :filters/number, :default 5})))
    (is (= [:op/ge 5]
           (sut/queryp->values {:code :filters/number, :default [:op/ge 5]})))
    (is (= [:op/_nil 1 2 3]
           (sut/queryp->values {:code :filters/number, :default [1 2 3]})))
    (is (= [:op/le 2]
           (sut/queryp->values {:code :filters/number, :default ["op/le" 2]})))
    (is (= [:op/eq 1 2 3]
           (sut/queryp->values {:code :filters/number, :default ["op/eq" 1 2 3]})))))

(deftest parse-param-key-test
  (testing "Can parse param key to extract name and operation"
    (is (= ["name" :op/_nil]
           (sut/parse-param-key "name")))
    (is (= ["age" :op/gt]
           (sut/parse-param-key "age:gt")))))

(deftest normalize-param-val-test
  (testing "Can normalize param value to get a flat str vector with operation as head"
    (is (= [:op/_nil "john"]
           (sut/normalize-param-val :op/_nil "john")))
    (is (= [:op/_nil "128"]
           (sut/normalize-param-val :op/_nil 128)))
    (is (= [:op/_nil "M" "F"]
           (sut/normalize-param-val :op/_nil ["M" "F"])))
    (is (= [:op/_nil "read,write"]
           (sut/normalize-param-val :op/_nil ["read,write"])))))

(deftest process-params-test
  (testing "Can process params map to extract operations"
    (is (= {"age" [:op/gt "21"]
            "name" [:op/_nil "john"]
            "gender" [:op/_nil "F" "M"]
            "version" [:op/_nil "5"]}
           (sut/process-params {"age:gt" "21"
                                "name" "john"
                                "gender" ["F" "M"]
                                "version" 5})))))

(deftest get-param-test
  (testing "Can extract param value and operation"
    (is (= [:op/_nil "35"]
           (sut/get-param {"age" [:op/_nil "35"]} {:name :age})))
    (is (= [:op/gt "21"]
           (sut/get-param {"age" [:op/gt "21"]} {:name :age})))
    (is (= [:op/lt "2"]
           (sut/get-param {"age" [:op/lt "2"]} {:name :age})))
    (is (= [:op/bw "8:00-17:30"]
           (sut/get-param {"time" [:op/bw "8:00-17:30"]} {:name :time})))
    (is (= [:op/_nil "8:00-17:30"]
           (sut/get-param {"time" [:op/_nil "8:00-17:30"]} {:name :time})))
    (is (= [:op/_nil]
           (sut/get-param {"foo" [:op/_nil"bar"]} {:name :baz})))
    (is (= [:op/_nil "128"]
           (sut/get-param {"_limit" [:op/_nil "128"]} {:name :_limit})))
    (is (= [:op/_nil "128"]
           (sut/get-param {"foo" [:op/_nil "bar"]} {:name :_limit, :default 128})))
    (is (= [:op/ge "21"]
           (sut/get-param {"foo" [:op/_nil "bar"]} {:name :_limit, :default [:op/ge 21]})))
    (is (= [:op/gt 35]
           (sut/get-param {"age" [:op/gt "35"]} {:name :age} #(Integer/parseInt %))))
    (is (= [:op/_nil 128]
           (sut/get-param {"foo" [:op/_nil "bar"]} {:name :_limit, :default 128} #(Integer/parseInt %))))))

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
