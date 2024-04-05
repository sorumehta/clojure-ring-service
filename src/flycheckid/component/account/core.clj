(ns flycheckid.component.account.core
  (:require [flycheckid.component.auth.core :as auth]
            [ring.util.http-response :as http-response]))



(defn sign-up
  [{:keys [cognito-idp]}
   {{{email :email password :password} :body} :parameters}]
  (let [cognito-account (auth/create-cognito-account cognito-idp {:email email :password password})]
    (if-not (contains? cognito-account :cognitect.anomalies/category)
      (http-response/ok {:account/account-id (:UserSub cognito-account)
                         :account/display-name email})
      (http-response/bad-request {:message (:message cognito-account)}))))

(defn confirm
  [{:keys [cognito-idp]}
   {{{email :email confirmation-code :confirmationCode} :body} :parameters}]
  (let [cognito-account (auth/confirm-account cognito-idp {:email email :confirmation-code confirmation-code})]
    (if-not (contains? cognito-account :cognitect.anomalies/category)
      (http-response/ok nil)
      (http-response/bad-request {:message (:message cognito-account)}))))

(defn login
  [{:keys [cognito-idp]}
   {{{email :email password :password} :body} :parameters}]
  (let [cognito-login (auth/login-account cognito-idp {:email email :password password})]
    (if-not (contains? cognito-login :cognitect.anomalies/category)
      (http-response/ok (:AuthenticationResult cognito-login))
      (http-response/bad-request {:message (:message cognito-login)}))))
