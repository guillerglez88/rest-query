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
(def pag-sort     :page/sort)

(defn not-implemented [sql-map _field _params _alias _def-val]
  sql-map)

(defn contains-text [sql-map field params alias _def-val]
  (let [[val _op] (util/get-param params field)]
    (filters/contains-text sql-map alias val)))

(defn match-exact [sql-map field params alias _def-val]
  (let [[val _op] (util/get-param params field)]
    (filters/match-exact sql-map alias val)))

(defn number [sql-map field params alias _def-val]
  (let [[val op] (util/get-param params field)]
    (filters/number sql-map alias (bigdec val) op)))

(defn page-start [sql-map field params _alias def-val]
  (let [[val] (util/get-param params field {:default def-val, :parser #(Integer/parseInt %)})]
    (filters/page-start sql-map val)))

(defn page-size [sql-map field params _alias def-val]
  (let [[val] (util/get-param params field {:default def-val, :parser #(Integer/parseInt %)})]
    (filters/page-size sql-map val)))

(defn page-sort [sql-map field params _alias def-val]
  (let [[val op] (util/get-param params field {:default def-val})]
    (filters/page-sort sql-map val op)))

(def filters-map
  {flt-text     contains-text
   flt-keyword  match-exact
   flt-url      not-implemented
   flt-number   number
   flt-date     not-implemented
   pag-offset   page-start
   pag-limit    page-size
   pag-sort     page-sort})

(defn refine [sql-map queryp params]
  (let [path (-> queryp :path (or []))
        field (-> path last :alias)
        def-val (:default queryp)
        [sql-map alias] (fields/extract-path sql-map path)
        filter (get filters-map (:code queryp))]
    (filter sql-map field params alias def-val)))

(defn make-sql-map [url-map queryps]
  (let [from (:from url-map)
        alias (util/make-alias from)
        sql-map (fields/all-by-type from alias)]
    (->> (identity queryps)
         (filter #(or (contains? % :default)
                      (contains? (:params url-map) (name (-> % :path last :alias)))))
         (reduce (fn [acc curr] (refine acc curr (:params url-map))) sql-map)
         (identity))))

(defn make-query [url-map queryps]
  (let [query (make-sql-map url-map queryps)
        total (filters/total query)
        fpage (hsql/format query {:numbered true})
        ftotal (hsql/format total {:numbered true})
        hash (util/calc-hash (first fpage))]
    (hash-map :from (:from url-map)
              :hash hash
              :page fpage
              :total ftotal)))

(defn url->query [url queryps]
  (-> url util/url->map (make-query queryps)))
