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
  (go)
  (require '[cognitect.aws.client.api :as aws])
  (keys state/system)
  (:auth/cognito state/system)



;; {:UserConfirmed false,
;;  :CodeDeliveryDetails {:Destination "s***@g***", :DeliveryMedium "EMAIL", :AttributeName "email"},
;;  :UserSub "670cb07f-14c4-4eae-ac9b-19a7a68a7c31"}

  ;; {:__type "UsernameExistsException",
  ;;  :message "An account with the given email already exists.",
  ;;  :cognitect.aws.http/status 400,
  ;;  :cognitect.anomalies/category :cognitect.anomalies/incorrect,
  ;;  :cognitect.aws.error/code "UsernameExistsException"}


  (halt))
