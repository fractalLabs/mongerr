(ns mongerr.core
  "Conección a BD"
  (:require [clj-time.core :as t]
            [environ.core :refer [env]]
            [monger.collection :as mc]
            [monger.command :as cmd]
            [monger.core :as mg]
            [monger.db :refer [get-collection-names]]
            monger.joda-time
            [monger.operators :refer :all]
            [nillib.tipo :refer :all]
            [nillib.worm :refer :all]))

(defn mongo-url
  ^{:private true}
  []
  (or (env :mongo-url)
      "mongodb://localhost/admin"))
(def ^{:private true :dynamic true}
  *conn-db* (mg/connect-via-uri (mongo-url)))
(def ^{:private true :dynamic true} *db* (:db *conn-db*))

(defn remove-ids
  "Remove :_id from mongo objects, because we yet don't handle them as json"
  [coll] ;WISH convert to proper date object
  (map #(dissoc (assoc % :created_at (.getTimestamp (:_id %))) :_id) coll))

(def blacklisted-collections
  "This are the collection names that wont be listed"
  ["users" "auths" "test" "dummy" "changes" "credentials" "messages"])

(defn valid-collection?
  "If collection is users return false"
  [collection]
  (and (not (some #{collection} blacklisted-collections))
       (not (or (zero? (.indexOf collection "system."))
                (zero? (.indexOf collection "fs."))))))

(defn collections
  "Get all the collection names"
  []
  (sort (filter valid-collection?
          (get-collection-names *db*))))

(defn db-find
  "Fetch items from collection"
  ([] (collections))
  ([collection] (db-find collection {}))
  ([collection where]
    (println "**Collection: " collection)
    (remove-ids (mc/find-maps *db* collection where)))
  ([collection where fields]
    (println "**Collection: " collection)
    (remove-ids (mc/find-maps *db* collection where fields))))

(defn db-findf
  "Find first result, use like db-find"
  [collection where]
  (mc/find-one-as-map *db* collection where))

(defn db-text-search
  "Search a collection in full text"
  [coll query]
  (db-find coll {$text {$search query}}))

(defn db-update
  "Update data in collection"
  [coll conditions document]
  (if-not (or (empty? conditions) (empty? document))
    (mc/update *db* coll conditions {$set document} {:return-new true :upsert true :multi true})))

(defn db-update-all
  "Update data to all elements of collection"
  [coll document]
  (mc/update *db* coll {} {$set document} {:return-new true :upsert true :multi true}))

(defn db-upsert
  "Upsert data in collection"
  [coll conditions document]
  (mc/upsert *db* coll conditions {$set document}))

(defn db-remove
  "Remove data in collection"
  [coleccion match]
  {:pre [(not-empty match)]}
  (mc/remove *db* coleccion match))

(defn db-delete
  "Delete all contents in a collection"
  [coll]
  (mc/remove *db* coll {}))

(defn db-insert
  ":ordered false: si falla al insertar uno, inserta los demas
  http://docs.mongodb.org/manual/reference/command/insert/#dbcmd.insert"
  [collection o]
  (if (map? o)
     (with-meta o {:db (first (:db (meta (db-insert collection [o]))))})
    (let [subcolls (partition-all 500 o)
          data (doall(map (fn [c] (mg/command *db*
                             (array-map :insert collection
                                        :documents (map #(assoc % :date_insert (java.util.Date.)) c)
                                        :ordered false)))
                    subcolls))]
      (with-meta o {:db data}))))

(defn db
  "Same as db-find
  This exists because find is the most common operation"
  [& args]
  (apply db-find args))

(defn db-take-1
  "Take one element in collection"
  [coleccion where]
  (mc/find-one-as-map *db* coleccion where))

(defn db-stats
  "Get db-stats"
  [] (cmd/db-stats *db*))

(defn coll-stats
  "Get stats from collections"
  ([] (map coll-stats (collections)))
  ([coll] (cmd/collection-stats *db* coll)))

(defn extract-coords [m] ;TODO this smells
  (let [v (filter number? (map read-string (vals m)))]
    [(first (filter neg? v))
     (first (filter pos? v))]))

(defn db-geo
  "Geographic db-find"
  ([collection coords radius]
    ;(remove-ids
      (db-find collection {:coords {:$near {:$geometry {:type "Point" :coordinates coords}
                                            :$maxDistance radius}}}));)
  ([collection coords]
   (db-geo collection coords 200)))

(defn serializable?
  "Is this object serializable?"[v]
  (instance? java.io.Serializable v))

(defn serialize
  "Serializes value, returns a byte array"
  [v]
  (let [buff (java.io.ByteArrayOutputStream. 1024)]
    (with-open [dos (java.io.ObjectOutputStream. buff)]
      (.writeObject dos v))
    (.toByteArray buff)))

(defn deserialize
  "Accepts a byte array, returns deserialized value"
  [bytes]
  (with-open [dis (java.io.ObjectInputStream.
                   (java.io.ByteArrayInputStream. bytes))]
    (.readObject dis)))

;; TODO this should be somewhere lse

(defn collection-metadata
  "Get metadata from fields in a collection.
  WARNING use only on small collections"  ;TODO choose subset when too big
  [collection]
  (let [data (digitalize (db-find collection))
        ks (distinct (mapcat keys data))
        t-y-st (tipo-y-subtipo data)]
    (assoc (zipmap ks (map #(metadata (map % data)) ks))
           :collection collection
           :count (count data))))

(defn save-collection-metadata
  [collection]
  (db-upsert :metadata
             {:collection collection}
             (collection-metadata collection)))

(defn collections-metadata
  "Update collections-metadata on all collections"
  []
  (pmap save-collection-metadata (db-find)))
