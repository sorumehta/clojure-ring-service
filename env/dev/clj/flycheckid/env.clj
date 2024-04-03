(ns flycheckid.env
  (:require
   [clojure.tools.logging :as log]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[flycheck starting using the development or test profile]=-"))
   :start      (fn []
                 (log/info "\n-=[flycheck started successfully using the development or test profile]=-"))
   :stop       (fn []
                 (log/info "\n-=[flycheck has shut down successfully]=-"))
   :middleware (fn [handler _] handler)
   :opts       {:profile       :dev
                :persist-data? true}})
