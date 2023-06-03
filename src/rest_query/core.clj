(ns rest-query.core
  (:require
   [rest-query.fields :as fields]
   [rest-query.filters :as filters]))

(def flt-text     :filters/text)
(def flt-keyword  :filters/keyword)
(def flt-url      :filters/url)
(def flt-number   :filters/number)
(def flt-date     :filters/date)
(def pag-offset   :page/offset)
(def pag-limit    :page/limit)

(defn not-implemented [sql-map _queryp _params]
  sql-map)

(defn contains-text [sql-map queryp params]
  (let [field (:name queryp)
        value (get params (name field))]
    (filters/contains-text sql-map field value)))

(defn match-exact [sql-map queryp params]
  (let [field (:name queryp)
        value (get params (name field))]
    (filters/match-exact sql-map field value)))

(defn page-start [sql-map queryp params]
  (let [field (:name queryp)
        value (-> params (get (name field)) (or (:value queryp)))]
    (filters/page-start sql-map value)))

(defn page-size [sql-map queryp params]
  (let [field (:name queryp)
        value (-> params (get (name field)) (or (:value queryp)))]
    (filters/page-size sql-map value)))

(def filters-map
  {flt-text     contains-text
   flt-keyword  match-exact
   flt-url      not-implemented
   flt-number   not-implemented
   flt-date     not-implemented
   pag-offset   page-start
   pag-limit    page-size})

(defn refine [sql-map base queryp params]
  (let [path (-> queryp :path (or []))
        alias (-> queryp :name (fields/make-alias))
        filter (get filters-map (:code queryp))]
    (-> (identity sql-map)
        (fields/extract-path base path alias)
        (filter queryp params))))

(defn make-sql-map [type base queryps params]
  (let [sql-map (fields/all-by-type type)]
    (->> (identity queryps)
         (reduce (fn [acc curr] (refine acc base curr params)) sql-map)
         (identity))))
