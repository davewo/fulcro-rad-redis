(ns com.fulcrologic.rad.database-adapters.redis-test
  (:require [taoensso.carmine :as car :refer [wcar]]
            [com.fulcrologic.rad.ids :as ids]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [com.fulcrologic.rad.database-adapters.redis :as redis]
            [com.fulcrologic.rad.test-schema.person :as person]
            [com.fulcrologic.rad.attributes :as attr]
            [com.fulcrologic.rad.form :as form]
            [fulcro-spec.core :refer [specification assertions component behavior when-mocking]])
  (:import (com.github.fppt.jedismock RedisServer)))

(declare _)

(def redis-port 54433)

(defn with-mock-redis [test]
  (let [mock-redis (RedisServer. redis-port)]
    (.start mock-redis)
    (test)
    (.stop mock-redis)))

(clojure.test/use-fixtures :each with-mock-redis)

(def all-attributes (vec (concat person/attributes)))

(def key->attribute (into {}
                      (map (fn [{::attr/keys [qualified-key] :as a}]
                             [qualified-key a]))
                      all-attributes))

(def env {::attr/key->attribute key->attribute
          ::redis/connections   {:redis {:host "localhost" :port redis-port}}})

(specification "redis CRUD"
  (let [conn         (-> env ::redis/connections :redis)
        constant-key (ids/new-uuid 1)
        random-value (ids/new-uuid)]
    (assertions
      "sets a value"
      (redis/set-value conn constant-key random-value) => "OK"
      "gets a value"
      (redis/get-value conn constant-key) => random-value)))

(specification "save-form!"
  (behavior "adds new attributes"
    (when-mocking
      (redis/set-value _ _ _) => "OK"
      (let [tempid1 (tempid/tempid (ids/new-uuid 1))
            delta   {[::person/id tempid1] {::person/id        tempid1
                                            ::person/full-name {:after "Bob"}}}]
        (assertions
          "remaps temp ids to real ids"
          (get-in (redis/save-form! env {::form/delta delta}) [:tempids tempid1]) =fn=> #(string? %)))))
  (behavior "updates existing attributes"
    (let [real-id         (str (ids/new-uuid 1))
          delta           {[::person/id real-id] {::person/id        real-id
                                                  ::person/full-name {:before "Bob" :after "Bobby Sue"}}}
          expected-update {::person/id        real-id
                           ::person/full-name "Bobby Sue"}]
      (when-mocking
        (redis/get-value _ id) => (do (assertions
                                        "by the correct ID"
                                        id => real-id)
                                      (assoc expected-update ::person/full-name "Bob"))
        (redis/set-value _ id val) => (do (assertions
                                            "with the new values"
                                            id => real-id
                                            val => expected-update))
        (assertions
          "remaps temp ids to real ids"
          (redis/save-form! env {::form/delta delta}) =fn=> #(map? %))))))

