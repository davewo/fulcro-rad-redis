(ns user
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.repl
     :as tools-ns
     :refer [disable-reload! refresh clear set-refresh-dirs]]
    [taoensso.timbre :as log]))

(set-refresh-dirs "src/main" "src/test" "src/dev")
