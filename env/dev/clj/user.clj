(ns user
  (:require [integrant.core :as ig]
            [integrant.repl.state :as state]
            [integrant.repl :refer [go halt reset]]
            [flycheckid.config]
            [flycheckid.core-service]
            [cognitect.aws.client.api :as aws]
            [datomic.api :as d]))



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

  (def q-result #{["b34c6968-e3e6-43c5-b593-4734423795c8" "soru.mehta@outlook.com" "Saurabh (Outlook)"] ["670cb07f-14c4-4eae-ac9b-19a7a68a7c31" "soru.mehta@gmail.com" "Saurabh Mehta"]})
  (first q-result)
  
  (halt))
