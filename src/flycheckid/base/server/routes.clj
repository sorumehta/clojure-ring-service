(ns flycheckid.base.server.routes
  (:require
   [integrant.core :as ig]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [flycheckid.base.server.controllers.health :as health]
   [flycheckid.base.server.middleware.exception :as exception]
   [flycheckid.base.server.middleware.formats :as formats]
   [flycheckid.component.account.core :as account]
   [flycheckid.component.account.spec :as account-spec]
   [flycheckid.base.server.middleware.auth :as auth]
   [ring.util.http-response :as http-response]
   [clojure.pprint :as pprint]))


(def route-data
  {:coercion   malli/coercion
   :muuntaja   formats/instance
   :swagger    {:id ::api}
   :middleware [;; query-params & form-params
                parameters/parameters-middleware
                  ;; content-negotiation
                muuntaja/format-negotiate-middleware
                  ;; encoding response body
                muuntaja/format-response-middleware
                  ;; exception handling
                coercion/coerce-exceptions-middleware
                  ;; decoding request body
                muuntaja/format-request-middleware
                  ;; coercing response bodys
                coercion/coerce-response-middleware
                  ;; coercing request parameters
                coercion/coerce-request-middleware
                  ;; exception handling
                exception/wrap-exception]})


(defn private-routes
  [opts]
  ["/private" {:middleware [(auth/wrap-token-authentication opts)]}
   ["/get-user-info" {:post {:handler (fn [request]
                                        (http-response/ok (:claims request)))}}]])


;; Routes
(defn public-routes
  [opts]
  [["/swagger.json"
    {:get {:no-doc  true
           :swagger {:info {:title "FlycheckID API"}}
           :handler (swagger/create-swagger-handler)}}]
   ["/health"
    {:get health/healthcheck!}]
   ["/account/sign-up"
    {:post {:summary "creates a user in aws cognito user pool"
            :parameters {:body [:map
                                [:email string?]
                                [:password string?]]}
            :responses {200 {:body account-spec/Account}}
            :handler (partial account/sign-up opts)}}]

   ["/account/confirm"
    {:post {:summary "confirms a user in aws cognito user pool"
            :parameters {:body [:map
                                [:email string?]
                                [:confirmationCode string?]]}
            :responses {200 {:body nil}}
            :handler (partial account/confirm opts)}}]
   ["/account/login"
    {:post {:summary "logs in a user in aws cognito user pool"
            :parameters {:body [:map
                                [:email string?]
                                [:password string?]]}
            :responses {200 {:body account-spec/Login}}
            :handler (partial account/login opts)}}]])



(defn api-routes
  [opts]
  ["/v1" (public-routes opts) (private-routes opts)])


(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  (fn [] [base-path route-data (api-routes opts)]))
