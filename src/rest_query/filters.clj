(ns rest-query.filters
  (:require
   [honey.sql.helpers :refer [limit offset select where]]
   [rest-query.fields :as fields]))

(defn contains-text [sql-map field value]
  (let [alias (fields/make-alias field)]
    (where sql-map [:like [:cast alias :text] (str "%" value "%")])))

(defn match-exact [sql-map field value]
  (let [alias (fields/make-alias field)]
    (where sql-map [:= [:cast alias :text] (str "\"" value "\"")])))

(defn page [sql-map start count]
  (-> sql-map
      (offset start)
      (limit count)))

(defn total [sql-map]
  (-> sql-map
      (dissoc :select :offset :limit)
      (select [[:count :*] :count])))
