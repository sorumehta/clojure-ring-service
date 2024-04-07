(ns user
  (:require [integrant.core :as ig]
            [integrant.repl.state :as state]
            [integrant.repl :refer [go halt reset]]
            [flycheckid.config]
            [flycheckid.core-service]
            [cognitect.aws.client.api :as aws]
            [datomic.api :as d]
            [flycheckid.component.database.core :as datomic]))



(println "Hello, welcome to the user namespace")

(defn dev-prep!
  []
  (integrant.repl/set-prep! (fn []
                              (-> (flycheckid.config/system-config {:profile :dev})
                                  (ig/prep)))))
(dev-prep!)


(comment
  (go)
  (reset)
  (keys state/system)
  (def conn (:db.datomic/conn state/system))
  (def db (d/db conn))
  (def get-account-q '[:find ?account-id ?email ?display-name
                       :in $ ?account-id
                       :where [?e :account/account-id ?account-id]
                       [?e :account/display-name ?display-name]
                       [?e :account/email ?email]])
  (require '[flycheckid.component.database.core :as datomic])
  (d/q get-account-q  db "b34c6968-e3e6-43c5-b593-4734423795c8")

  (defn test-args
    [q d & more]
    (apply println more))

  (test-args 1 2)

  (halt))
