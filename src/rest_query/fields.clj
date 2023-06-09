(ns rest-query.fields
  (:require
   [honey.sql.helpers :refer [from inner-join select]]
   [honey.sql.pg-ops :refer [at>]]
   [rest-query.util :as util]
   [clojure.string :as str]))

(defn contains-alias? [sql-map alias]
  (->> (:inner-join sql-map)
       (filter vector?)
       (some #(-> % second (= alias)))
       (boolean)))

(defn all-by-type [type alias]
  (let [all-fields (-> alias name (str ".*") keyword)]
    (-> (select all-fields)
        (from [type alias]))))

(defn extract-prop [sql-map base path-elem]
  (let [field (:field path-elem)
        alias (:alias path-elem)]
    (inner-join sql-map [[:jsonb_extract_path base [:inline field]] alias] true)))

(defn extract-coll [sql-map base path-elem]
  (let [alias (:alias path-elem)]
    (inner-join sql-map
                [[:jsonb_array_elements base] alias]
                (if-let [filter (:filter path-elem)]
                  [[at> alias [:lift filter]]]
                  true))))

(defn link-entity [sql-map base path-elem]
  (let [alias (:alias path-elem)
        [ename efield] (->> (str/split (:link path-elem) #"\/")
                            (filter (complement str/blank?)))
        field-ref (-> alias name (str "." efield) (keyword))]
    (inner-join sql-map [(keyword ename) alias] [:= [:concat [:inline (str "/" ename "/")] field-ref]
                                                    [:cast base :TEXT]])))

(defn extract-field [sql-map base path-elem]
  (let [already-included? (contains-alias? sql-map (:alias path-elem))
        linked-entity? (contains? path-elem :link)
        table-column? (:root path-elem)
        collection-prop? (-> path-elem :coll boolean)]
    (cond
      already-included? (identity sql-map)
      linked-entity?    (link-entity sql-map base path-elem)
      table-column?     (identity sql-map)
      collection-prop?  (extract-coll sql-map base path-elem)
      :else             (extract-prop sql-map base path-elem))))

(defn extract-path [sql-map path]
  (loop [acc sql-map
         base nil
         [curr & more] (util/prepare-path path)]
    (if (nil? curr)
      (vector acc base)
      (-> (extract-field acc base curr)
          (recur (:alias curr) more)))))
