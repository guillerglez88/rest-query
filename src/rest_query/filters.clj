(ns rest-query.filters
  (:require
   [honey.sql.helpers :refer [limit offset order-by select where]]
   [rest-query.util :as util]))

(def op-nil   :op/_nil)
(def op-eq    :op/eq)
(def op-le    :op/le)
(def op-lt    :op/lt)
(def op-ge    :op/ge)
(def op-gt    :op/gt)
(def op-asc   :op/asc)
(def op-desc  :op/desc)

(def num-op-map
  {op-eq  :=
   op-le  :<=
   op-lt  :<
   op-ge  :>=
   op-gt  :>})

(def sort-op-map
  {op-asc   nil
   op-desc  :desc})

(defn contains-text [sql-map field value]
  (let [alias (util/make-alias field)]
    (where sql-map [:like [:cast alias :TEXT] (str "%" value "%")])))

(defn match-exact [sql-map field value]
  (let [alias (util/make-alias field)]
    (where sql-map [:= [:cast alias :TEXT] (str "\"" value "\"")])))

(defn number [sql-map field value op]
  (let [alias (util/make-alias field)
        sql-op (get num-op-map (or op op-eq))]
    (where sql-map [sql-op [:cast alias :DECIMAL] value])))

(defn page-start [sql-map start]
  (offset sql-map start))

(defn page-size [sql-map count]
  (limit sql-map count))

(defn page-sort [sql-map field op]
  (let [alias (util/make-alias field)
        sql-op (get sort-op-map (or op op-asc))]
    (order-by sql-map [alias (when sql-op sql-op)])))

(defn page [sql-map start count]
  (-> (identity sql-map)
      (page-start start)
      (page-size count)))

(defn total [sql-map]
  (-> sql-map
      (dissoc :select :offset :limit)
      (select [[:count :*] :count])))
