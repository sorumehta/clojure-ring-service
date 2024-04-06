

(ns flycheckid.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [integrant.core :as ig]))


(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defmethod aero/reader 'ig/refset
  [_ _ value]
  (ig/refset value))

(defn read-config
  [filename options]
  (aero/read-config (io/resource filename) options))

(defmethod ig/init-key :system/env [_ env] env)

(def ^:const system-filename "system.edn")

(defn system-config
  [options]
  (let [config (read-config system-filename options)]
    config))
