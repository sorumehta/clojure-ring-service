(ns flycheckid.component.account.spec)
(def Account
  [:map
   [:account/account-id string?]
   [:account/display-name string?]])