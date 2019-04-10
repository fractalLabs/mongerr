(ns mongerr.core
  "Conección a BD"
  (:require [clj-time.core :as t]
            [compojure.core :refer :all]
            [comun.core :refer :all]
            [environ.core :refer [env]]
            [digitalize.core :refer :all]
            [monger.collection :as mc]
            [monger.command :as cmd]
            [monger.conversion :refer [from-db-object]]
            [monger.core :as mg]
            [monger.db :refer [get-collection-names]]
            monger.joda-time
            monger.json
            [monger.operators :refer :all])
  (:import [com.mongodb Bytes]))

;; Connection
(def local-mongo-url "mongodb://localhost/admin")

(defn mongo-url
  ^{:private true}
  []
  (or (env :mongo-url)
      local-mongo-url))

(def ^{:dynamic true}
  *conn-db* (mg/connect-via-uri (mongo-url)))

(def ^{:dynamic true} *db* (:db *conn-db*))

(defn remove-ids
  "Remove :_id from mongo objects, because we yet don't handle them as json"
  [coll] ;WISH convert to proper date object
  (map #(dissoc % :_id) coll))

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

;; Crud

(defn db-find
  "Fetch items from collection"
  ([] (collections))
  ([collection]
   (db-find collection {}))
  ([collection where]
    (printerr "**Collection: " collection ", where: " where)
   (remove-ids (let [db-cur (mc/find *db* collection where)]
                 (.setOptions db-cur Bytes/QUERYOPTION_NOTIMEOUT)
                 (map #(from-db-object %1 true) db-cur))))
  ([collection where fields]
   (printerr "**Collection: " collection ", where: " where)
    (remove-ids (mc/find-maps *db* collection where fields))))

(defn db
  "Same as db-find
  This exists because find is the most common operation"
  [& args]
  (apply db-find args))

(defn db-insert- [collection elements]
  (try
    (mg/command *db*
                (array-map :insert collection
                           :documents elements ;:documents (map #(assoc % :date-insert (java.util.Date.)) elements)
                           :ordered false))
    (catch Exception e (if (seq? elements) (doall (map #(db-insert- collection %)
                                                      elements))))))

(defn db-insert
  ":ordered false: si falla al insertar uno, inserta los demas
  http://docs.mongodb.org/manual/reference/command/insert/#dbcmd.insert"
  [collection o]
  (if (map? o)
    (db-insert collection [o])
    (let [subcolls (partition-all 100 (remove-ids o))
          data (doall (map #(db-insert- collection %)
                           subcolls))]
      (printerr "inserted on " collection ", " data)
      data)))

(defn db-findf
  "Find first result, use like db-find"
  ([collection]
   (db-findf collection {}))
  ([collection where]
   (mc/find-one-as-map *db* collection where)))

(defn db-text-search
  "Search a collection in full text"
  [coll query]
  (db-find coll {$text {$search query}}))

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

(defn db-update
  "Update data in collection"
  [collection conditions document]
  (if-not (or (empty? conditions) (empty? document))
    (mc/update *db*
               collection
               conditions
               {$set (assoc document :date-update (t/now))}
               {:return-new true :upsert true :multi true})))

(defn db-update-all
  "Update data to all elements of collection"
  [collection document]
  (mc/update *db* collection {} {$set document} {:return-new true :upsert true :multi true}))

(defn db-upsert
  "Upsert data in collection"
  [collection conditions document]
  (mc/upsert *db* collection conditions {$set document}))

(defn db-remove
  "Remove data in collection"
  [collection match]
  {:pre [(not-empty match)]}
  (mc/remove *db* collection match))

(defn db-delete
  "Delete all contents in a collection"
  [collection]
  (mc/remove *db* collection {}))

(defn db-drop
  "Delete collection from database"
  [collection]
  (mc/drop *db* collection))

(defn db-rename
  [old-collection new-collection]
  (mc/rename *db* (name old-collection) (name new-collection)))

(defn coll-tmp [coll] (keyword (str (name coll) "-tmp")))
(defn coll-tmp-rand [] (str "tmp-" (rand)))

(defn data-snapshot
  [coll o]
  (let [data (doall (remove-nils (if (fn? o) (o) o)))
        tmp1 (coll-tmp-rand)
        tmp2 (coll-tmp-rand)]
    (when (seq data)
      (db-insert tmp1 data)
      (if (not (empty? (db coll)))
        (db-rename coll tmp2))
      (db-rename tmp1 coll)
      (db-drop tmp2))))

;; Stats

(defn db-stats
  "Get db-stats"
  [] (cmd/db-stats *db*))

(defn coll-stats
  "Get stats from collections"
  ([] (map coll-stats (collections)))
  ([coll] (cmd/collection-stats *db* coll)))

(defn db-count
  "Count documents in collection"
  ([coll]
   (db-count coll nil))
  ([coll conditions]
   (mc/count *db* coll conditions)))


;; Serialization

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

;;

(def crud-routes
  "Add this routes to your app handler for the crud rest api"
  [(GET "/db/:collection" [& collection]
        (db-find collection))
   ()])
