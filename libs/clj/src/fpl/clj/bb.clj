(ns fpl.clj.bb)

(defmacro if-bb
  [then else]
  (if (System/getProperty "babashka.version")
    then
    else))