(ns rest-query.util
  (:require
   [clojure.string :as str]
   [lambdaisland.uri :as uri :refer [query-string->map uri]])
  (:import
   [java.security MessageDigest]))

(defn url->map [url]
  (let [uri-map (uri url)
        type (->> (str/split (:path uri-map) #"\/")
                  (filter (complement str/blank?))
                  (first))
        params (-> uri-map :query (query-string->map {:keywordize? false}))]
    (hash-map :from (keyword type)
              :params params)))

(defn parse-param-key [key]
  (let [[fst snd] (->> (str/split key #":" 2)
                       (filter (complement str/blank?)))]
    (if (nil? snd)
      (vector fst)
      (vector fst (keyword "op" snd)))))

(defn process-params [params]
  (->> (seq params)
       (map (fn [[k v]]
              (-> (parse-param-key k)
                  (#(vector (first %) (vector (str v) (second %)))))))
       (into {})))

(defn get-param
  ([params queryp]
   (get-param params queryp nil))
  ([params queryp parser]
   (let [key (:name queryp)
         default (-> queryp :default str (vector nil))
         parse (or parser identity)
         [val op] (-> params (get (name key)) (or default))]
     (-> val parse (vector op)))))

(defn calc-hash [payload]
  (let [sha256 (MessageDigest/getInstance "SHA-256")]
    (->> (.getBytes payload "UTF-8")
         (.digest sha256)
         (map (partial format "%02x"))
         (apply str))))

(defn make-alias [& parts]
  (->> parts
       (filter (complement nil?))
       (map name)
       (map str/lower-case)
       (filter (complement str/blank?))
       (map #(str/replace % #"[-.]" "_"))
       (map #(str/replace % #"_+$" ""))
       (str/join "_")
       (str/trimr)
       (keyword)))

(defn assign-alias [path-elem parent]
  (let [field (-> path-elem :field name)
        base (if (nil? parent) "" (-> parent :alias name (str ".")))]
    (-> (cond
          (:coll path-elem)             (make-alias base "elem")
          (contains? path-elem :link)   (make-alias base "entity")
          (contains? path-elem :alias)  (-> path-elem :alias make-alias)
          (:root path-elem)             (-> base (str field) keyword)
          :else                         (make-alias base field))
        ((partial assoc path-elem :alias)))))

(defn expand-elem [path-elem parent]
  (let [link? (contains? path-elem :link)
        coll? (contains? path-elem :coll)
        root? (or (nil? parent)
                  (contains? parent :link))
        self (assoc path-elem :root root?)]
    (cond
      link? (vector (dissoc self :link) self)
      coll? (vector (dissoc self :coll) self)
      :else (vector self))))

(defn prepare-path [path]
  (->> (seq path)
       (reduce #(concat %1 (expand-elem %2 (last %1))) [])
       (reduce #(conj %1 (assign-alias %2 (last %1))) [])
       (vec)))
