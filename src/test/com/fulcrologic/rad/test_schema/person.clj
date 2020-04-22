(ns com.fulcrologic.rad.test-schema.person
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]))

(defattr id ::id :string
  {::attr/identity?     true
   ::attr/schema        :redis})

(defattr full-name ::full-name :string
  {::attr/schema     :redis
   ::attr/identities #{::id}
   ::attr/required?  true})

(def attributes [id full-name])