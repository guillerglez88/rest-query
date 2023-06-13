(ns rest-query.filters
  (:require
   [honey.sql.helpers :refer [limit offset order-by select where]]
   [rest-query.util :as util]
   [clojure.string :as str]))

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
   op-gt  :>
   op-nil :=})

(def sort-op-map
  {op-asc   nil
   op-desc  :desc
   op-nil   nil})

(defn contains-text [sql-map field values]
  (let [alias (util/make-alias field)]
    (->> (or values [])
         (filter (complement str/blank?))
         (reduce #(where %1 [:like [:cast alias :TEXT] (str "%" %2 "%")]) sql-map))))

(defn match-exact [sql-map field values]
  (let [alias (util/make-alias field)
        str-values (->> (or values [])
                        (filter (complement str/blank?))
                        (map #(str "\"" % "\"")))]
    (condp = (count str-values)
      0 (identity sql-map)
      1 (where sql-map [:= [:cast alias :TEXT] (first str-values)])
      (where sql-map [:in [:cast alias :TEXT] str-values]))))

(defn number [sql-map field values op]
  (let [alias (util/make-alias field)
        sql-op (get num-op-map (or op op-eq))]
    (->> (or values [])
         (map bigdec)
         (reduce #(where %1 [sql-op [:cast alias :DECIMAL] %2]) sql-map))))

(defn page-start [sql-map start]
  (offset sql-map (Integer/parseInt start)))

(defn page-size [sql-map count]
  (limit sql-map (Integer/parseInt count)))

(defn page-sort [sql-map values op renames]
  (let [sql-op (get sort-op-map (or op op-asc))]
    (->> (identity values)
         (map (partial get renames))
         (reduce #(order-by %1 [%2 (when sql-op sql-op)]) sql-map))))

(defn page [sql-map start count]
  (-> (identity sql-map)
      (page-start start)
      (page-size count)))

(defn total [sql-map]
  (-> sql-map
      (dissoc :select :offset :limit :order-by)
      (select [[:count :*] :count])))
