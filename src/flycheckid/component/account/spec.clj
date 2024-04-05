(ns flycheckid.component.account.spec)
(def Account
  [:map
   [:account/account-id string?]
   [:account/display-name string?]])

(def Login
  [:map
   [:AccessToken string?]
   [:ExpiresIn number?]
   [:TokenType string?]
   [:RefreshToken string?]
   [:IdToken string?]])