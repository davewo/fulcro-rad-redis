(ns com.fulcrologic.rad.test-schema.prescription
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]))

(defattr id ::id :string
  {::attr/identity?     true
   ::attr/schema        :redis})

(defattr right-sphere ::right-sphere :int
  {::attr/schema     :redis
   ::attr/identities #{::id}
   ::attr/required?  false})

(def attributes [id right-sphere])