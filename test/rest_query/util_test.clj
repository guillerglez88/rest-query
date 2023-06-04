(ns rest-query.util-test
  (:require [rest-query.util :as sut]
            [clojure.test :as t :refer [deftest testing is]]))

(deftest uri->map
  (testing "Can convert uri into a map with the data needed for query"
    (is (= {:from "Person"
            :params {"fname" "john"
                     "lname" "doe"
                     "gender" "M"
                     "_offset" "0"
                     "_limit" "5"}}
           (sut/url->map "/Person?fname=john&lname=doe&gender=M&_offset=0&_limit=5")))))
