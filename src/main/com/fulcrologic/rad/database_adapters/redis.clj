(ns com.fulcrologic.rad.database-adapters.redis
  (:require
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [taoensso.timbre :as log]
    [taoensso.carmine :as car :refer [wcar]]
    [com.fulcrologic.rad.ids :as ids]))


(comment
  (def sample-redis-connection
    {:pool {} :spec {:host "localhost" :port 6379}})
  (wcar {:host "localhost" :port 6379}
    (car/set "zuz" {:squib :jib})
    (car/get "zuz"))
  (wcar {:pool {} :spec {:host "localhost" :port 6379}}
    (car/get "zuz")))

(defn test-mocking [conn k v]
  (wcar conn (car/set k v))
  (wcar conn (car/get k)))

(defn redis-running? [connection]
  (try
    (= "PONG" (wcar connection (car/ping)))
    (catch Exception e false)))

(defn init-connections [config]
  (-> config ::connections))

(defn id-attr? [{::attr/keys [key->attribute]} [k v]]
  (log/info k v)
  (log/info key->attribute)
  (::attr/identity? (key->attribute k)))


(defn save-form! [{::keys [connections] :as env} {::form/keys [delta]}]
  (let [temp-ids->real-ids (reduce-kv
                             (fn [m [k id :as ident] v]
                               (if (tempid/tempid? id) (assoc m id (str (ids/new-uuid))) m))
                             {} delta)
        redis-entries      (map
                             (fn [[k v]]
                               (let [redis-key (get temp-ids->real-ids (second k))
                                     redis-val (cond-> v
                                                 (get v (first k)) (assoc (first k) redis-key))
                                     redis-val (reduce-kv
                                                 (fn [m k {:keys [after] :as v}]
                                                   (assoc m k (if after after v)))
                                                 {} redis-val)]
                                 [redis-key redis-val])) delta)]
    (doseq [[entry-k entry-v] redis-entries]
      (wcar (:redis connections) (car/set entry-k entry-v)))
    {:tempids temp-ids->real-ids}))
