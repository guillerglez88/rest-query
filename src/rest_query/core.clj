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

(def filters-map
  {flt-text     (fn [sql-map  alias  val _op _renames] (filters/contains-text  sql-map alias val))
   flt-keyword  (fn [sql-map  alias  val _op _renames] (filters/match-exact    sql-map alias val))
   flt-number   (fn [sql-map  alias  val  op _renames] (filters/number         sql-map alias val op))
   pag-offset   (fn [sql-map _alias  val _op _renames] (filters/page-start     sql-map (first val)))
   pag-limit    (fn [sql-map _alias  val _op _renames] (filters/page-size      sql-map (first val)))
   pag-sort     (fn [sql-map _alias  val  op  renames] (filters/page-sort      sql-map val op renames))
   flt-url      (fn [sql-map _alias _val _op _renames] (identity               sql-map))
   flt-date     (fn [sql-map _alias _val _op _renames] (identity               sql-map))})

(defn refine [sql-map queryp params renames]
  (let [path (-> queryp :path (or []))
        code (-> queryp :code keyword)
        alias (get renames (queryp :name))
        [op & values] (util/get-param params queryp)
        filter (code filters-map)]
    (-> (fields/extract-path sql-map path)
        (filter alias values op renames))))

(defn make-sql-map [from params queryps]
  (let [alias (util/make-alias from)
        sql-map (fields/all-by-type from alias)
        queryps-exp (map util/expand-queryp queryps)
        renames (->> queryps-exp (map #(vector (:name %) (:alias %))) (into {}))
        params-exp (util/process-params params)]
    (->> (identity queryps-exp)
         (filter #(or (contains? % :default)
                      (contains? params-exp (:name %))))
         (reduce (fn [acc curr] (refine acc curr params-exp renames)) sql-map))))

(defn make-query [from params queryps]
  (let [query (make-sql-map from params queryps)
        total (filters/total query)
        fpage (hsql/format query {:numbered true})
        ftotal (hsql/format total {:numbered true})
        hash (util/calc-hash (first fpage))]
    (hash-map :from from
              :hash hash
              :page fpage
              :total ftotal)))

(defn url->query [url queryps]
  (let [url-map (util/url->map url)
        from (:from url-map)
        params (:params url-map)]
    (make-query from params queryps)))
