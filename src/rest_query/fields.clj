(ns rest-query.fields
  (:require
   [clojure.string :as str]
   [honey.sql.helpers :refer [from inner-join select]]
   [honey.sql.pg-ops :refer [at>]]))

(defn contains-alias? [sql-map alias]
  (->> (:inner-join sql-map)
       (filter vector?)
       (some #(-> % second (= alias)))
       (boolean)))

(defn make-alias [& parts]
  (->> parts
       (filter (complement nil?))
       (map name)
       (filter (complement str/blank?))
       (map #(str/replace % #"-" "_"))
       (str/join "_")
       (str/trimr)
       (keyword)))

(defn all-by-type [type]
    (-> (select :res.*)
        (from [type :res])))

(defn extract-prop [sql-map base path-elem alias]
  (let [field (:prop path-elem)]
    (if (contains-alias? sql-map alias)
      (identity sql-map)
      (inner-join sql-map [[:jsonb_extract_path base field] alias] true))))

(defn extract-coll [sql-map base path-elem alias]
  (let [field (:prop path-elem)
        prop-alias (make-alias base field)]
    (if (contains-alias? sql-map alias)
      (identity sql-map)
      (-> (extract-prop sql-map base path-elem prop-alias)
          (inner-join [[:jsonb_array_elements prop-alias] alias]
                      (if-let [filter (:filter path-elem)]
                        [[at> alias [:lift filter]]]
                        true))))))

(defn extract-field [sql-map base path-elem alias]
  (cond
    (:meta path-elem) sql-map ;; TODO: implement meta fields access
    (:coll path-elem) (extract-coll sql-map base path-elem alias)
    :else             (extract-prop sql-map base path-elem alias)))

(defn extract-path [sql-map base path alias]
  (let [[curr & more] path
        curr-name (:prop curr)
        suffix (when (:coll curr) "elem")
        curr-alias (if (empty? more) alias (make-alias base curr-name suffix))]
    (if (nil? curr)
      sql-map
      (-> (extract-field sql-map base curr curr-alias)
          (extract-path curr-alias more alias)))))
