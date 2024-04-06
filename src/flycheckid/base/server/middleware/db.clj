(ns flycheckid.base.server.middleware.db
  (:require [datomic.api :as d]))


(defn wrap-database
  [{:keys [conn]}]
  (fn [handler]
    (fn [request]
      (println (str "datomic connection: " conn))
      (let [db (d/db conn)]
        (handler (assoc request :db db))))))
      
 

