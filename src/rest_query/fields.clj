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
  (let [field (:field path-elem)]
    (inner-join sql-map [[:jsonb_extract_path base field] alias] true)))

(defn extract-coll [sql-map base path-elem alias]
  (let [field (:field path-elem)
        prop-alias (make-alias base field)]
    (-> (extract-prop sql-map base path-elem prop-alias)
        (inner-join [[:jsonb_array_elements prop-alias] alias]
                    (if-let [filter (:filter path-elem)]
                      [[at> alias [:lift filter]]]
                      true)))))

(defn extract-field [sql-map base path-elem alias]
  (let [already-included? (contains-alias? sql-map alias)
        table-column? (nil? base)
        collection-prop? (-> path-elem :coll boolean)]
    (cond
      already-included? (identity sql-map)
      table-column?     (identity sql-map)
      collection-prop?  (extract-coll sql-map base path-elem alias)
      :else             (extract-prop sql-map base path-elem alias))))

(defn extract-path [sql-map path alias]
  (loop [acc sql-map
         base nil
         [curr & more] path
         alias alias]
    (let [curr-name (:field curr)
          suffix (when (:coll curr) "elem")
          curr-alias (if (empty? more) alias (make-alias base curr-name suffix))]
      (if (nil? curr)
        acc
        (-> (extract-field acc base curr curr-alias)
            (recur curr-alias more alias))))))
