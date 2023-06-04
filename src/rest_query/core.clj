(ns rest-query.core
  (:require
   [honey.sql :as hsql]
   [rest-query.fields :as fields]
   [rest-query.filters :as filters]
   [rest-query.util :as util]))

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
        [val _op] (util/get-param params field)]
    (filters/contains-text sql-map field val)))

(defn match-exact [sql-map queryp params]
  (let [field (:name queryp)
        [val _op] (util/get-param params field)]
    (filters/match-exact sql-map field val)))

(defn number [sql-map queryp params]
  (let [field (:name queryp)
        [val op] (util/get-param params field)]
    (filters/number sql-map field (bigdec val) op)))

(defn page-start [sql-map queryp params]
  (let [field (:name queryp)
        [val] (util/get-param params field {:default (:default queryp)
                                            :parser #(Integer/parseInt %)})]
    (filters/page-start sql-map val)))

(defn page-size [sql-map queryp params]
  (let [field (:name queryp)
        [val] (util/get-param params field {:default (:default queryp)
                                            :parser #(Integer/parseInt %)})]
    (filters/page-size sql-map val)))

(def filters-map
  {flt-text     contains-text
   flt-keyword  match-exact
   flt-url      not-implemented
   flt-number   number
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

(defn make-sql-map [url-map base queryps]
  (let [sql-map (-> url-map :from fields/all-by-type)]
    (->> (identity queryps)
         (filter #(or (contains? % :default)
                      (contains? (:params url-map) (name (:name %)))))
         (reduce (fn [acc curr] (refine acc base curr (:params url-map))) sql-map)
         (identity))))

(defn make-query [url-map base queryps]
  (let [query (make-sql-map url-map base queryps)
        total (filters/total query)]
    (hash-map :from (:from url-map)
              :page (hsql/format query {:numbered true})
              :total (hsql/format total {:numbered true}))))

(defn url->query [url base queryps]
  (-> url util/url->map (make-query base queryps)))
