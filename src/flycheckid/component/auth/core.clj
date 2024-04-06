(ns flycheckid.component.auth.core
  (:require [clojure.data.json :as json]
            [cognitect.aws.client.api :as aws]
            [integrant.core :as ig]
            [flycheckid.component.log.interface :as log])
  (:import [com.auth0.jwk UrlJwkProvider GuavaCachedJwkProvider]
           [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [com.auth0.jwt.interfaces RSAKeyProvider]
           [com.auth0.jwt.exceptions JWTDecodeException]
           [java.util Base64]
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

(defn split-token
  "Splits a JWT into its three segments (header, payload, signature) based on '.' delimiters.
   Throws an exception if the token format is invalid."
  [token]
  (when-not token
    (throw (JWTDecodeException. "Token cannot be null")))

  (let [parts (.split token "\\.")]  ;; Split on the '.' character (escape the period)
    (if (= (count parts) 3)
      parts
      (throw (JWTDecodeException. (format "Invalid JWT format: Expected 3 parts, found %s" (count parts)))))))

(defn validate-signature
  [{:keys [key-provider]} token]
  (let [algorithm (Algorithm/RSA256 key-provider)
        verifier (.build (JWT/require algorithm))]
    (.verify verifier token))
  (let [parts (split-token token)]
    (get parts 1)))


(defn decode-to-str
  [s]
  (log/info (str "token to decode " s))
  (String. (.decode (Base64/getUrlDecoder) s)))

(defn decode-token
  [token]
  (-> token
      (decode-to-str)
      (json/read-str)))

(defn verify-payload
  [config {:strs [client_id iss token_use] :as payload}]
  (when-not
   (and
    (= (:client-id config) client_id)
    (= (:jwks config) iss)
    (contains? #{"access" "id"} token_use))
    (throw (ex-info "Token verification failed" {})))
  payload)

(defn verify-and-get-payload
  [auth token]
  (->> token
       (validate-signature auth)
       (decode-token)
       (verify-payload auth)))

;; Integrant lifecycle functions
(defmethod ig/init-key :auth/cognito
  [_ opts]
  (let [key-provider (-> (:jwks opts)
                         (UrlJwkProvider.)
                         (GuavaCachedJwkProvider.))]

    (merge opts
           {:cognito-client (aws/client {:api :cognito-idp})
            :key-provider (reify RSAKeyProvider
                            (getPublicKeyById [_ kid]
                              (.getPublicKey (.get key-provider kid)))
                            (getPrivateKey [_]
                              nil)
                            (getPrivateKeyId [_]
                              nil))})))
