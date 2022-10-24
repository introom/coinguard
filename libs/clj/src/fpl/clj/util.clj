(ns fpl.clj.util)

(defn qualified-name
  [n]
  (-> symbol str))

(comment
  (bytes? (byte-array [1 2 3])))