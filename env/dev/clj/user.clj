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
  @(d/transact conn [{:db/doc "Hello world"}])

  (def movie-schema [{:db/ident :movie/title
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The title of the movie"}

                     {:db/ident :movie/genre
                      :db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/doc "The genre of the movie"}

                     {:db/ident :movie/release-year
                      :db/valueType :db.type/long
                      :db/cardinality :db.cardinality/one
                      :db/doc "The year the movie was released in theaters"}])

  @(d/transact conn movie-schema)

  (def first-movies [{:movie/title "The Goonies"
                      :movie/genre "action/adventure"
                      :movie/release-year 1985} ;; three attributes, making up an entity
                     {:movie/title "Commando"
                      :movie/genre "action/adventure"
                      :movie/release-year 1985}
                     {:movie/title "Repo Man"
                      :movie/genre "punk dystopia"
                      :movie/release-year 1984}])

  @(d/transact conn first-movies)


  (defn get-email-from-user-attributes [user-data]
    (let [email-attrs (->> user-data
                           (get "UserAttributes")  ; Use get with string key
                           (filter #(= (get % "Name") "email")))]
      (if-not (empty? email-attrs)
        (-> email-attrs first (get "Value"))  ; Use get with string key
        "Email not found")))


  (def attrs {"Username" "670cb07f-14c4-4eae-ac9b-19a7a68a7c31",
              "UserAttributes" [{"Name" "sub",
                                 "Value" "670cb07f-14c4-4eae-ac9b-19a7a68a7c31"},
                                {"Name" "email_verified",
                                 "Value" "true"},
                                {"Name" "email",
                                 "Value" "soru.mehta@gmail.com"}]})
  (get-email-from-user-attributes attrs)
  (halt))
