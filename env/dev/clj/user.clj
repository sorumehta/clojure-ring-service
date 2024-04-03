(ns user
  (:require [cognitect.aws.client.api :as aws])
  (:import [java.util Base64]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))


(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algo "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes client-secret) hmac-sha256-algo)
        mac (Mac/getInstance hmac-sha256-algo)
        _ (.init mac signing-key)
        _ (.update mac (.getBytes username))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (Base64/getEncoder) raw-hmac)))

(calculate-secret-hash
 {:client-id "client-id"
  :client-secret "client-secret"
  :username "username"})

(def cognito-idp (aws/client {:api :cognito-idp}))
(aws/ops cognito-idp)

(aws/doc cognito-idp :SignUp)

(aws/validate-requests cognito-idp true)

(aws/invoke cognito-idp
            {:op :SignUp
             :request {:ClientId "1011uh8vd4hoaec7dpc5k7l7e7"
                       :Username "saurabh@flycheckid.com"
                       :Password "bhsh2B!g"
                       :SecretHash ""}})

(println "Hello, welcome to the namespace")
