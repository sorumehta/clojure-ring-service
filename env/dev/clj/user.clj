(ns user
  (:require [integrant.core :as ig]
            [integrant.repl.state :as state]
            [integrant.repl :refer [go halt reset]]
            [flycheckid.component.auth.core :refer [calculate-secret-hash]]
            [flycheckid.config]
            [flycheckid.core-service]
            [cognitect.aws.client.api :as aws]))



;; (calculate-secret-hash
;;  {:client-id "client-id"
;;   :client-secret "client-secret"
;;   :username "username"})

;; (def cognito-idp (aws/client {:api :cognito-idp}))
;; (aws/ops cognito-idp)

;; (aws/doc cognito-idp :SignUp)



(println "Hello, welcome to the user namespace")

(defn dev-prep!
  []
  (integrant.repl/set-prep! (fn []
                              (-> (flycheckid.config/system-config {:profile :dev})
                                  (ig/prep)))))
(dev-prep!)


(comment
  (+ 1 1 3 4)
  (go)
  (require '[flycheckid.component.auth.core :refer [verify-payload]])
  (keys state/system)
  (verify-payload
   (:auth/cognito state/system)
   {"event_id" "c2b4baad-d1ae-49ad-8548-ede5d7a3c815", "iat" 1712384694, "auth_time" 1712384694, "sub" "b34c6968-e3e6-43c5-b593-4734423795c8", "origin_jti" "41591802-9dc7-4c1b-bf2d-d62d10e1f452", "username" "b34c6968-e3e6-43c5-b593-4734423795c8", "iss" "https://cognito-idp.ap-south-1.amazonaws.com/ap-south-1_aanfpLpqB", "token_use" "access", "jti" "d47cdd0c-63df-4621-87e8-0bdab9f23760", "exp" 1712388294, "scope" "aws.cognito.signin.user.admin", "client_id" "45n3v093jmci7c270r658hgfi5"})



;; {:UserConfirmed false,
;;  :CodeDeliveryDetails {:Destination "s***@g***", :DeliveryMedium "EMAIL", :AttributeName "email"},
;;  :UserSub "670cb07f-14c4-4eae-ac9b-19a7a68a7c31"}

  ;; {:__type "UsernameExistsException",
  ;;  :message "An account with the given email already exists.",
  ;;  :cognitect.aws.http/status 400,
  ;;  :cognitect.anomalies/category :cognitect.anomalies/incorrect,
  ;;  :cognitect.aws.error/code "UsernameExistsException"}

`
  (halt))
