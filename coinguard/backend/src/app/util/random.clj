(ns app.util.random
  (:import java.security.SecureRandom
           java.util.Base64))

(defn secure-string
  ([]
   ;;  16 bytes (128 bits) is safe as we do have aes-128
   (secure-string 16))
  ([buf-len]
   (let [encoder (.withoutPadding (Base64/getUrlEncoder))
         random (SecureRandom.)
         buf (byte-array buf-len)]
     (.nextBytes random buf)
     (.encodeToString encoder buf))))

(comment
  (secure-string))