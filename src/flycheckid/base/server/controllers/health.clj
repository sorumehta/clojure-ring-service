
(ns flycheckid.base.server.controllers.health
  (:require
   [ring.util.http-response :as http-response])
  (:import
   [java.util Date]))

(defn healthcheck!
  [_req]
  (http-response/ok
   {:time     (str (Date. (System/currentTimeMillis)))
    :up-since (str (Date. (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean))))
    :app      {:status  "up"
               :message "I am healthy, thanks for asking"}}))
