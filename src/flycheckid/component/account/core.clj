(ns flycheckid.component.account.core
  (:require [flycheckid.component.auth.core :as auth]
            [flycheckid.component.log.interface :as log]
            [clojure.pprint :as pprint]
            [flycheckid.component.database.core :as datomic]
            [flycheckid.shared.utils :as utils]))


(defn list-accounts
  [_opts
   {db :db}]
  ;; TODO: Check permission using claims
  (let [all-accounts-q '[:find ?account-id ?email ?display-name
                         :where [?e :account/account-id ?account-id]
                         [?e :account/display-name ?display-name]
                         [?e :account/email ?email]]
        query-result (datomic/query all-accounts-q db)]
    {:status 200 :body (utils/bind-list-to-map query-result
                                               [:account-id :email :display-name])}))

(defn create-account
  [{:keys [cognito-idp conn]}
   {{{name :name} :body} :parameters headers :headers}]
  (let [user-attrs (auth/get-user-attrs cognito-idp headers)]
    (if-let [user-email (auth/get-user-attr-value user-attrs "email")]
      (do (let [result (datomic/transact-data conn [{:account/account-id (:Username user-attrs)
                                                     :account/display-name name
                                                     :account/email user-email}])]
            (log/info result))
          {:status 200 :body {:account-id (:Username user-attrs)}})
      {:status 500 :body {:message "Error while creating user"}})))


(defn get-account-by-token
  [{:keys [cognito-idp]}
   {headers :headers db :db}]
  (let [user-attrs (auth/get-user-attrs cognito-idp headers)]
    (let [get-account-q '[:find ?account-id ?email ?display-name
                          :in $ ?account-id
                          :where [?e :account/account-id ?account-id]
                          [?e :account/display-name ?display-name]
                          [?e :account/email ?email]]
          query-result (datomic/query get-account-q db (:Username user-attrs))]
      (if-not (empty? query-result)
        {:status 200 :body (first (utils/bind-list-to-map query-result
                                                          [:account-id :email :display-name]))}
        {:status 404 :body nil}))))



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
