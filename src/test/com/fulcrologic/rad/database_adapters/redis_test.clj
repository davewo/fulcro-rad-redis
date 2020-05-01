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

(defn with-mock-redis [test]
  (let [mock-redis (RedisServer. 12312)]
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
          ::redis/connections   {:redis {:host "localhost" :port 12312}}})

(specification "save-form!"
  (behavior "test mock"
    (let [id            (str (ids/new-uuid))
          conn          {}
          redis-ok      "OK"
          redis-get-val {:zuz :jix}
          delta         {[::person/id id] {::person/id        id
                                           ::person/full-name {:before "Jim" :after "Bob"}}}]
      (assertions
        "does shit"
        (redis/test-mocking {:host "localhost" :port 12312} "foo" redis-get-val) => {})
      ;(when-mocking
      ;  (wcar conn redis-ok) =1x=> redis-ok
      ;  (wcar conn redis-get-val) =1x=> redis-get-val
      ;  (car/set k v) => redis-ok
      ;  (car/get k) => redis-get-val
      ;  (assertions
      ;    "remaps temp ids to real ids"
      ;    (redis/test-mocking {} "foo" redis-get-val) => redis-get-val))
      ))
  ;(behavior "adds new attributes"
  ;  (let [tempid1 (tempid/tempid (ids/new-uuid 1))
  ;        delta   {[::person/id tempid1] {::person/id        tempid1
  ;                                        ::person/full-name {:after "Bob"}}}]
  ;    (when-mocking
  ;      (wcar _ _) => "OK"
  ;      (car/set k v) =>
  ;      (assertions
  ;        "updates redis"
  ;        k =fn=> #(string? %)
  ;        v => {::person/id        k
  ;              ::person/full-name "Bob"})
  ;      (assertions
  ;        "remaps temp ids to real ids"
  ;        (get-in (redis/save-form! env {::form/delta delta}) [:tempids tempid1]) =fn=> #(string? %)))))
  )

