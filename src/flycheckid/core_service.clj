(ns flycheckid.core-service
  (:require
   [integrant.core :as ig]
   [flycheckid.component.log.interface :as log]
   [flycheckid.config :as config]
   [flycheckid.env :refer [defaults]]
   [flycheckid.base.server.core]
   [flycheckid.base.server.handler]
   [flycheckid.base.server.routes]
   [flycheckid.component.auth.core]
   [flycheckid.component.database.core])
  (:gen-class))



;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(defonce system (atom nil))

(defn stop-app []
  ((or (:stop defaults) (fn [])))
  (some-> (deref system) (ig/halt!))
  (shutdown-agents))

(defn start-app [& [params]]
  ((or (:start params) (:start defaults) (fn [])))
  (log/init)
  (->> (config/system-config (or (:opts params) (:opts defaults) {}))
       (ig/prep)
       (ig/init)
       (reset! system))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& _]
  (start-app))
