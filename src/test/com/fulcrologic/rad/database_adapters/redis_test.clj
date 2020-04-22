(ns com.fulcrologic.rad.database-adapters.redis-test
  (:require [taoensso.carmine :as car :refer [wcar]]
            [com.fulcrologic.rad.ids :as ids]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [com.fulcrologic.rad.database-adapters.redis :as redis]
            [com.fulcrologic.rad.test-schema.person :as person]
            [com.fulcrologic.rad.attributes :as attr]
            [com.fulcrologic.rad.form :as form]
            [fulcro-spec.core :refer [specification assertions component behavior when-mocking]]))

(declare _)

(def all-attributes (vec (concat person/attributes)))

(def key->attribute (into {}
                      (map (fn [{::attr/keys [qualified-key] :as a}]
                             [qualified-key a]))
                      all-attributes))

(specification "save-form!"
  (behavior "adds new attributes"
    (let [env     {::attr/key->attribute key->attribute
                   ::redis/connections   {:redis {:host "localhost" :port 6379}}}
          tempid1 (tempid/tempid (ids/new-uuid 1))
          delta   {[::person/id tempid1] {::person/id        tempid1
                                          ::person/full-name {:after "Bob"}}}]
      (when-mocking
        (wcar _ _) => ["OK"]
        (car/set k v) =>
        (assertions
          "updates redis"
          k =fn=> #(string? %)
          v => {::person/id        k
                ::person/full-name "Bob"})
        (assertions
          "remaps temp ids to real ids"
          (get-in (redis/save-form! env {::form/delta delta}) [:tempids tempid1]) =fn=> #(string? %))))))

