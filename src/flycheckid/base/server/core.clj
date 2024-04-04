(ns flycheckid.base.server.core
  (:require [clojure.pprint :as pprint]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [s-exp.hirundo :as hirundo]))



(defn start [handler {:keys [port] :as opts}]
  (try
    (println "options to server:")
    (pprint/pprint opts)
    (let [server (hirundo/start!
                  handler
                  opts)]
      (log/info "server started on port" port)
      server)
    (catch Throwable t
      (log/error t (str "server failed to start on port: " port)))))

(defn stop [server]
  (hirundo/stop! server)
  (log/info "HTTP server stopped"))

;; Integrant lifecycle functions

(defmethod ig/prep-key :server/http
  [_ config]
  (merge {:port 3000
          :host "0.0.0.0"}
         config))

(defmethod ig/init-key :server/http
  [_ opts]
  ;; I don't know the reason behind using delay here. Too bad!
  (let [handler (atom (delay (:http-handler opts)))]
    {:http-handler handler
     :server  (start (fn [req] (@@handler req)) (dissoc opts :http-handler))}))

(defmethod ig/halt-key! :server/http
  [_ {:keys [server]}]
  (stop server))

(defmethod ig/suspend-key! :server/http
  [_ {:keys [handler]}]
  (reset! handler (promise)))


;; I have no idea what this function is doing, but kit framework does it, so I do it!
(defmethod ig/resume-key :server/http
  [k opts old-opts old-impl]
  (if (= (dissoc opts :http-handler) (dissoc old-opts :http-handler))
    (do (deliver @(:http-handler old-impl) (:http-handler opts))
        old-impl)
    (do (ig/halt-key! k old-impl)
        (ig/init-key k opts))))

