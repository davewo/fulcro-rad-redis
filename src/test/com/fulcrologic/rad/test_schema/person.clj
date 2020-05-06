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

(defattr prescription ::prescription :ref
  {::attr/schema     :redis
   ::attr/target     :com.fulcrologic.rad.test-schema.prescription/id
   ::attr/identities #{::id}})

(def attributes [id full-name])