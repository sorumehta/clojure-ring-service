(ns flycheckid.component.account.core
  (:require [flycheckid.component.auth.core :as auth]
            [flycheckid.component.log.interface :as log]
            [clojure.pprint :as pprint]))



(defn create-account
  [{:keys [cognito-idp]}
   {{{name :name} :body} :parameters db :db headers :headers}]
  (let [account-details (auth/get-user-attrs cognito-idp headers)])
  {:status 200 :body nil})

(defn get-user
  [{:keys [cognito-idp]}
   {:keys [headers]}]
  (let [cognito-account (auth/get-user-attrs cognito-idp headers)]
    {:status 200 :body cognito-account}))


(defn sign-up
  [{:keys [cognito-idp]}
   {{{email :email password :password} :body} :parameters}]
  (let [cognito-account (auth/create-cognito-account cognito-idp {:email email :password password})]
    {:status 200 :body cognito-account}))

(defn confirm
  [{:keys [cognito-idp]}
   {{{email :email confirmation-code :confirmationCode} :body} :parameters}]
  (auth/confirm-account cognito-idp {:email email :confirmation-code confirmation-code})
  {:status 200})

(defn resend-confirmation
  [{:keys [cognito-idp]}
   {{{email :email} :body} :parameters}]
  (auth/resend-confirmation cognito-idp {:email email})
  {:status 200})

(defn login
  [{:keys [cognito-idp]}
   {{{email :email password :password} :body} :parameters}]
  (let [cognito-login (auth/login-account cognito-idp {:email email :password password})]
    {:status 200 :body cognito-login}))

(defn refresh-token
  [{:keys [cognito-idp]}
   {{{refresh-token :refreshToken} :body} :parameters claims :claims}]
  (let [refresh-token-resp (auth/cognito-refresh-token cognito-idp
                                                       {:refresh-token refresh-token :sub (get-in claims ["sub"])})]
    {:status 200 :body refresh-token-resp}))

(defn add-to-group
  [{:keys [cognito-idp]}
   {{{groupName :groupName} :body} :parameters claims :claims}]
  (let [update-resp (auth/cognito-add-user-to-group cognito-idp claims groupName)]
    {:status 200 :body update-resp}))

(defn delete-user
  [{:keys [cognito-idp]}
   {claims :claims}]
  (let [delete-resp (auth/cognito-delete-user cognito-idp claims)]
    {:status 200 :body delete-resp}))
