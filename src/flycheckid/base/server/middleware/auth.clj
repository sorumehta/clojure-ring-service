(ns flycheckid.base.server.middleware.auth
  (:require [clojure.string :as string]
            [flycheckid.component.log.interface :as log]
            [flycheckid.component.auth.core :as auth]))

(defn get-token
  [req]
  (-> req
      (get-in [:headers "authorization"])
      (string/split #" ")
      (second)))

(defn wrap-token-authentication
  [{:keys [cognito-idp]}]
  (fn [handler]
    (fn
      [request]
      (let [claims (auth/verify-and-get-payload cognito-idp (get-token request))]
        (log/info (str "authentication successful with claims: " claims))
        (handler (assoc request :claims claims))))))
      
 


