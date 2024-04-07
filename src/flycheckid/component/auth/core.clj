(ns flycheckid.component.auth.core
  (:require [clojure.data.json :as json]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [cognitect.aws.client.api :as aws]
            [integrant.core :as ig])
  (:import [com.auth0.jwk GuavaCachedJwkProvider UrlJwkProvider]
           [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [com.auth0.jwt.exceptions JWTVerificationException]
           [com.auth0.jwt.interfaces RSAKeyProvider]
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


(defn when-anomaly-throw
  [result]
  (when (contains? result :cognitect.anomalies/category)
    (throw (ex-info (:__type result) {:type :system.exception/unauthorized
                                      :message (:message result)}))))

(defn get-token
  [headers]
  (-> headers
      (get-in ["authorization"])
      (string/split #" ")
      (second)))

(defn get-user-attrs
  [{:keys [cognito-client]} headers]
  (let [token (get-token headers)
        result (aws/invoke cognito-client
                           {:op :GetUser
                            :request
                            {:AccessToken token}})]
    (when-anomaly-throw result)
    result))

;; helper function to filter user attrs map
(defn get-user-attr-value
  [attrs-map attr-name]
  (when-let [rec (first (filter (fn [r]
                                  (= (r :Name) attr-name)) (attrs-map :UserAttributes)))]
    (rec :Value)))

(defn create-cognito-account
  [{:keys [cognito-client client-id client-secret]} {:keys [email password]}]
  (let [result (aws/invoke cognito-client
                           {:op :SignUp
                            :request
                            {:ClientId client-id
                             :Username email
                             :Password password
                             :SecretHash (calculate-secret-hash
                                          {:client-id client-id
                                           :client-secret client-secret
                                           :username email})}})]
    (when-anomaly-throw result)
    {:account/account-id (:UserSub result)
     :account/display-name email}))

(defn login-account
  [{:keys [cognito-client client-id client-secret user-pool-id]} {:keys [email password]}]
  (let [result (aws/invoke cognito-client
                           {:op :AdminInitiateAuth
                            :request
                            {:ClientId client-id
                             :UserPoolId user-pool-id
                             :AuthFlow "ADMIN_USER_PASSWORD_AUTH"
                             :AuthParameters {"USERNAME" email
                                              "PASSWORD" password
                                              "SECRET_HASH" (calculate-secret-hash
                                                             {:client-id client-id
                                                              :client-secret client-secret
                                                              :username email})}}})]
    (when-anomaly-throw result)
    (:AuthenticationResult result)))



(defn confirm-account
  [{:keys [cognito-client client-id client-secret]} {:keys [email confirmation-code]}]
  (let [result (aws/invoke cognito-client
                           {:op :ConfirmSignUp
                            :request {:ClientId client-id
                                      :Username email
                                      :ConfirmationCode confirmation-code
                                      :SecretHash (calculate-secret-hash
                                                   {:client-id client-id
                                                    :client-secret client-secret
                                                    :username email})}})]
    (when-anomaly-throw result)))

(defn resend-confirmation
  [{:keys [cognito-client client-id client-secret]} {:keys [email]}]
  (let [result (aws/invoke cognito-client
                           {:op :ResendConfirmationCode
                            :request {:ClientId client-id
                                      :Username email
                                      :SecretHash (calculate-secret-hash
                                                   {:client-id client-id
                                                    :client-secret client-secret
                                                    :username email})}})]
    (when-anomaly-throw result)))

(defn cognito-refresh-token
  [{:keys [cognito-client client-id client-secret user-pool-id]} {:keys [refresh-token sub]}]
  (let [result (aws/invoke cognito-client
                           {:op :AdminInitiateAuth
                            :request
                            {:ClientId client-id
                             :UserPoolId user-pool-id
                             :AuthFlow "REFRESH_TOKEN_AUTH"
                             :AuthParameters {"REFRESH_TOKEN" refresh-token
                                              "SECRET_HASH" (calculate-secret-hash
                                                             {:client-id client-id
                                                              :client-secret client-secret
                                                              :username sub})}}})]
    (when-anomaly-throw result)
    (:AuthenticationResult result)))


(defn cognito-add-user-to-group
  [{:keys [cognito-client client-id client-secret user-pool-id]} claims group-name]
  (let [{:strs [sub]} claims
        result (aws/invoke cognito-client
                           {:op :AdminAddUserToGroup
                            :request
                            {:ClientId client-id
                             :UserPoolId user-pool-id
                             :Username sub
                             :GroupName group-name
                             :SecretHash (calculate-secret-hash
                                          {:client-id client-id
                                           :client-secret client-secret
                                           :username sub})}})]
    (when-anomaly-throw result)
    (pprint/pprint result)
    (:AuthenticationResult result)))


(defn cognito-delete-user
  [{:keys [cognito-client user-pool-id]} claims]
  (let [{:strs [sub]} claims
        result (aws/invoke cognito-client
                           {:op :AdminDeleteUser
                            :request
                            {:UserPoolId user-pool-id
                             :Username sub}})]
    (when-anomaly-throw result)))




(defn split-token
  [token]
  (when-not token
    (throw (ex-info "JWTDecodeException" {:type :system.exception/unauthorized
                                          :message "no token found"})))

  (let [parts (.split token "\\.")]  ;; Split on the '.' character (escape the period)
    (if (= (count parts) 3)
      parts
      (throw (ex-info "JWTDecodeException" {:type :system.exception/unauthorized
                                            :message "Invalid token format"})))))

(defn validate-signature
  [{:keys [key-provider]} token]
  (try
    (let [algorithm (Algorithm/RSA256 key-provider)
          verifier (.build (JWT/require algorithm))]
      (.verify verifier token))
    (catch JWTVerificationException e
      (throw (ex-info "JWTVerificationException" {:type :system.exception/unauthorized
                                                  :message "unauthorized access"}))))
  (let [parts (split-token token)]
    (get parts 1)))


(defn decode-to-str
  [s]
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

(defn get-cognito-client-secret
  []
  (get-in (aws/invoke (aws/client {:api :ssm})
                      {:op :GetParameter
                       :request {:Name "cognito-client-secret"}})
          [:Parameter :Value]))

;; Integrant lifecycle functions
(defmethod ig/init-key :auth/cognito
  [_ opts]
  (let [key-provider (-> (:jwks opts)
                         (UrlJwkProvider.)
                         (GuavaCachedJwkProvider.))]

    (merge opts
           {:cognito-client (aws/client {:api :cognito-idp})
            :client-secret (get-cognito-client-secret)
            :key-provider (reify RSAKeyProvider
                            (getPublicKeyById [_ kid]
                              (.getPublicKey (.get key-provider kid)))
                            (getPrivateKey [_]
                              nil)
                            (getPrivateKeyId [_]
                              nil))})))
