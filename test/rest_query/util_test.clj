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
