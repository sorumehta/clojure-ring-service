(ns flycheckid.component.auth.core
  (:require [cognitect.aws.client.api :as aws]
            [integrant.core :as ig])
  (:import [java.util Base64]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))


(defn calculate-secret-hash
  [{:keys [client-id client-secret username]}]
  (let [hmac-sha256-algo "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes client-secret) hmac-sha256-algo)
        mac (doto (Mac/getInstance hmac-sha256-algo)
              (.init signing-key)
              (.update (.getBytes username)))
        raw-hmac (.doFinal mac (.getBytes client-id))]
    (.encodeToString (Base64/getEncoder) raw-hmac)))

(defn create-cognito-account
  [{:keys [cognito-client client-id client-secret]} {:keys [email password]}]
  (aws/invoke cognito-client
              {:op :SignUp
               :request {:ClientId client-id
                         :Username email
                         :Password password
                         :SecretHash (calculate-secret-hash
                                      {:client-id client-id
                                       :client-secret client-secret
                                       :username email})}}))

(defn login-account
  [{:keys [cognito-client client-id client-secret user-pool-id]} {:keys [email password]}]
  (aws/invoke cognito-client
              {:op :AdminInitiateAuth
               :request {:ClientId client-id
                         :UserPoolId user-pool-id
                         :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                         :AuthParameters {"USERNAME" email
                                          "PASSWORD" password
                                          "SECRET_HASH" (calculate-secret-hash
                                                         {:client-id client-id
                                                          :client-secret client-secret
                                                          :username email})}}}))

(defn confirm-account
  [{:keys [cognito-client client-id client-secret]} {:keys [email confirmation-code]}]
  (aws/invoke cognito-client
              {:op :ConfirmSignUp
               :request {:ClientId client-id
                         :Username email
                         :ConfirmationCode confirmation-code
                         :SecretHash (calculate-secret-hash
                                      {:client-id client-id
                                       :client-secret client-secret
                                       :username email})}}))

;; Integrant lifecycle functions
(defmethod ig/init-key :auth/cognito
  [_ opts]
  (merge opts
         {:cognito-client (aws/client {:api :cognito-idp})}))
