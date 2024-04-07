(ns flycheckid.shared.utils)

(defn bind-list-to-map
  [list-data list-of-keys]
  (mapv (fn [data-row] (zipmap list-of-keys data-row)) list-data))