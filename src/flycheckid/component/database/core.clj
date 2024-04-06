(ns flycheckid.component.database.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [flycheckid.component.log.interface :as log]
            [integrant.core :as ig]))


(defn ident-has-attr?
  [db ident attr]
  (contains? (d/pull db '[*] ident) attr))

(defn load-dataset
  [conn]
  (let [db (d/db conn)]
    (when-not (ident-has-attr? db :account/account-id :db/ident)
      (let [schema-data (-> (io/resource "schema.edn")
                            (slurp)
                            (edn/read-string))]
        @(d/transact conn schema-data)))))

;; Integrant lifecycle functions
(defmethod ig/init-key :db.datomic/conn
  [_ {:keys [db-uri]}]
  (log/info (str "db-uri: " db-uri))
  (when (d/create-database db-uri)
    (log/info (str "database " db-uri " created (did not exist)")))
  (let [conn (d/connect db-uri)]
    (load-dataset conn)
    conn))

(defmethod ig/halt-key! :db.datomic/conn
  [_ conn]
  (d/release conn))