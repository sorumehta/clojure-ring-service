(ns flycheckid.base.server.middleware.auth
  (:require [clojure.string :as string]
            [flycheckid.component.log.interface :as log]
            [flycheckid.component.auth.core :as auth]
            [ring.util.http-response :as http-response])
  (:import
   [com.auth0.jwt.exceptions JWTDecodeException JWTVerificationException]))

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
      (log/info "Performing authentication in middleware ")
      (try
        (let [claims (auth/verify-and-get-payload cognito-idp (get-token request))]
          (log/info (str "authentication successful with claims: " claims))
          (handler (assoc-in request [:claims] claims)))
        (catch JWTDecodeException de (do
                                       (log/info (str "JWTDecodeException occurred: " de))
                                       (http-response/unauthorized)))
        (catch JWTVerificationException ve (do
                                             (log/info (str "JWTVerificationException occurred: " ve))
                                             (http-response/unauthorized)))))))
      
 


