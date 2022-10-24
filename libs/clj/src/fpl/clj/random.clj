(ns fpl.clj.random
  (:import (java.security SecureRandom)))

(def ^:private secure-random (SecureRandom.))

(defn random-bytes
  "Returns a secure random byte sequence of the specified size."
  [size]
  (let [buffer (byte-array size)]
    (.nextBytes secure-random buffer)
    buffer))

(def ^:private -alphabet
  (into [] (map str) "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))

;; see https://zelark.github.io/nano-id-cc/ to calculate the collision probability.
(defn id
  ([size]
   (id size -alphabet))
  ([size alphabet]
   (let [alpha-len (count -alphabet)]
     (loop [bytes (random-bytes size)
            id    (StringBuilder.)]
       (if bytes
         (recur (next bytes)
                (let [ch (nth -alphabet (mod (first bytes) alpha-len))]
                  (.append id ch)))
         (str id))))))

(comment
  (random-bytes 160)
  ;; using a length of `20` is a great default, see https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html#session-id-length

  (id 20))

