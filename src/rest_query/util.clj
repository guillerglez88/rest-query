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
       (str/join "_")
       (str/trimr)
       (keyword)))

(defn assign-alias [base path-elem]
  (let [field (:field path-elem)
        has-alias? (contains? path-elem :alias)
        root? (or (nil? base) (:root path-elem))
        suffix (cond
                 (:link path-elem)  "entity"
                 (:coll path-elem)  "elem"
                 :else              nil)]
    (-> (cond
          has-alias?  (-> path-elem :alias (make-alias suffix))
          root?       (keyword field)
          :else       (make-alias base field suffix))
        ((partial assoc path-elem :alias)))))

(defn expand-elem [path-elem]
  (let [link? (contains? path-elem :link)
        coll? (contains? path-elem :coll)]
    (cond
      link? (vector (dissoc path-elem :link) path-elem)
      coll? (vector (dissoc path-elem :coll) path-elem)
      :else (vector path-elem))))

(defn prepare-path [path]
  (loop [base nil
         acc []
         [curr & more] path]
    (if (nil? curr)
      (vec acc)
      (let [path-elems (->> (expand-elem curr)
                            (map (partial assign-alias base)))]
        (recur (-> path-elems first :alias)
               (concat acc path-elems)
               more)))))
