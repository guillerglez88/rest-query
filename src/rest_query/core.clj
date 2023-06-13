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

(defn expand-params [params]
  (->> (seq params)
       (map (fn [[k v]]
              (let [[name op] (util/parse-param-key k)
                    val (util/normalize-param-val op v)]
                (vector name val))))
       (into {})
       (#(with-meta % {:expanded? true}))))

(defn expand-queryps [queryps]
  (->> (map util/expand-queryp queryps)
       (vec)
       (#(with-meta % {:expanded? true}))))

(defn make-sql-map [from params queryps]
  (let [alias (util/make-alias from)
        xparams (-> params meta :expanded? (if params (expand-params params)))
        xqueryps (-> queryps meta :expanded? (if queryps (expand-queryps queryps)))
        renames (->> xqueryps (map #(vector (:name %) (:alias %))) (into {}))
        sql-map (fields/all-by-type from alias)]
    (->> (identity xqueryps)
         (filter #(or (contains? % :default) (contains? xparams (:name %))))
         (reduce (fn [acc curr] (refine acc curr xparams renames)) sql-map))))

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
  (let [{from :from, params :params} (util/url->map url)]
    (make-query from params queryps)))
