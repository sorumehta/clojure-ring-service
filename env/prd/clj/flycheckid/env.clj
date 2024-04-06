(ns flycheckid.env
  (:require [flycheckid.component.log.interface :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[flycheck starting]=-"))
   :start      (fn []
                 (log/info "\n-=[flycheck started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[flycheck has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile :prod}})
