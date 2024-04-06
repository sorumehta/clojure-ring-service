(ns flycheckid.component.log.interface
  (:require [flycheckid.component.log.config :as config]
            [flycheckid.component.log.core :as core]))

(defn init []
  (config/init))

(defmacro info [& args]
  `(core/info ~args))

(defmacro warn [& args]
  `(core/warn ~args))

(defmacro error [& args]
  `(core/error ~args))
