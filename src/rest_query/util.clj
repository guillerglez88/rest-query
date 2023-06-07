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

(defn get-param [params key & {:keys [default parser]
                               :or {default nil, parser identity}}]
  (let [value (-> params (get (name key)) (or default) (str))
        [fst snd] (->> (str/split value #":" 2)
                       (filter (complement str/blank?))
                       (filter #(not= "esc" %)))]
    (if (nil? snd)
      (vector (parser fst))
      (vector (parser snd) (keyword "op" fst)))))

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
        suffix (when (:coll path-elem) "elem")]
    (-> (cond
          has-alias?  (-> path-elem :alias keyword)
          root?       (keyword field)
          :else       (make-alias base field suffix))
        ((partial assoc path-elem :alias)))))

(defn expand-elem [path-elem]
  (let [link? (contains? path-elem :link)]
    (if link?
      (vector (dissoc path-elem :link) path-elem)
      (vector path-elem))))

(defn prepare-path [path]
  (loop [base nil
         acc []
         [curr & more] path]
    (if (nil? curr)
      (vec acc)
      (let [path-elem (assign-alias base curr)
            expanded (expand-elem path-elem)]
        (recur (:alias path-elem)
               (concat acc expanded)
               more)))))
