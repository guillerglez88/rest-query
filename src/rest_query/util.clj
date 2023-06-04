(ns rest-query.util
  (:require
   [clojure.string :as str]
   [lambdaisland.uri :as uri :refer [query-string->map uri]]))

(defn url->map [url]
  (let [uri-map (uri url)
        type (->> (str/split (:path uri-map) #"\/")
                  (filter (complement str/blank?))
                  (first))
        params (-> uri-map :query (query-string->map {:keywordize? false}))]
    (hash-map :from (keyword type)
              :params params)))

(defn get-param [params key & {:keys [default parser]
                               :or {default nil, parser identity}}]
  (let [value (-> params (get (name key)) (or default) (str))
        [fst snd] (->> (str/split value #":" 2)
                       (filter (complement str/blank?))
                       (filter #(not= "esc" %)))]
    (if (nil? snd)
      (vector (parser fst))
      (vector (parser snd) (keyword "op" fst)))))
