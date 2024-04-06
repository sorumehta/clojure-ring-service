(ns flycheckid.component.log.config
  (:require [taoensso.timbre :as timbre]))


(defn init []
  (timbre/set-config! {:level     :info})
  (timbre/info "Initialized logging. Using console to print logs"))

