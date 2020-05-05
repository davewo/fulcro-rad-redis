(ns com.fulcrologic.rad.database-adapters.redis
  (:require
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [taoensso.timbre :as log]
    [taoensso.carmine :as car :refer [wcar]]
    [taoensso.encore :as enc]
    [com.fulcrologic.rad.ids :as ids]))


(comment
  (def sample-redis-connection
    {:pool {} :spec {:host "localhost" :port 6379}})
  (wcar {:host "localhost" :port 6379}
    (car/set "zuz" {:squib :jib})
    (car/get "zuz"))
  (wcar {:pool {} :spec {:host "localhost" :port 6379}}
    (car/get "zuz")))

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


(defn set-value [conn k v]
  (wcar conn (car/set k v)))

(defn get-value [conn k]
  (wcar conn (car/get k)))

(defn save-form! [{::keys [connections] :as env} {::form/keys [delta]}]
  (let [conn                  (:redis connections)
        updated-attrs         (reduce-kv
                                (fn [m [k id :as ident] v]
                                  (if (and (string? id) (not (tempid/tempid? id)))
                                    (let [without-delta (reduce-kv (fn [m k {:keys [after] :as v}]
                                                                     (assoc m k (if after after v))) {} v)]
                                      (assoc m id without-delta))
                                    m)) {} delta)
        existing-redis-values (some->> updated-attrs keys
                                (reduce
                                  (fn [acc k]
                                    (assoc acc k (get-value conn k))) {}))

        updated-redis-values  (merge-with merge existing-redis-values updated-attrs)
        temp-ids->real-ids    (reduce-kv
                                (fn [m [k id :as ident] v]
                                  (if (tempid/tempid? id) (assoc m id (str (ids/new-uuid))) m))
                                {} delta)
        new-redis-values      (reduce
                                (fn [acc [k v]]
                                  (enc/if-let [redis-key (get temp-ids->real-ids (second k))
                                               redis-val (cond-> v
                                                           (get v (first k)) (assoc (first k) redis-key))
                                               redis-val (reduce-kv
                                                           (fn [m k {:keys [after] :as v}]
                                                             (assoc m k (if after after v)))
                                                           {} redis-val)]
                                    (assoc acc redis-key redis-val)
                                    acc))
                                {} delta)
        all-redis-values      (merge new-redis-values updated-redis-values)]
    (doseq [[entry-k entry-v] all-redis-values]
      (set-value conn entry-k entry-v))
    {:tempids temp-ids->real-ids}))
