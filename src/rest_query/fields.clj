(ns rest-query.fields
  (:require
   [honey.sql.helpers :refer [from inner-join select]]
   [honey.sql.pg-ops :refer [at>]]
   [rest-query.util :as util]))

(defn contains-alias? [sql-map alias]
  (->> (:inner-join sql-map)
       (filter vector?)
       (some #(-> % second (= alias)))
       (boolean)))

(defn all-by-type [type alias]
  (let [all-fields (-> alias name (str ".*") keyword)]
    (-> (select all-fields)
        (from [type alias]))))

(defn extract-prop [sql-map base path-elem alias]
  (let [field (:field path-elem)]
    (inner-join sql-map [[:jsonb_extract_path base [:inline field]] alias] true)))

(defn extract-coll [sql-map base path-elem alias]
  (let [field (:field path-elem)
        prop-alias (util/make-alias base field)]
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
    (let [field (:field curr)
          suffix (when (:coll curr) "elem")
          curr-alias (if (empty? more) alias (util/make-alias base field suffix))]
      (if (nil? curr)
        acc
        (-> (extract-field acc base curr curr-alias)
            (recur curr-alias more alias))))))
