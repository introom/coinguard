(ns app.util.resource
  (:require-macros [app.util.resource])
  (:require
   ["react-native" :refer [Image]]))

;; NOTE: only works for ios.
;; see https://github.com/IgorBelyayev/React-Native-Local-Resource/blob/master/index.js#L7
(defn fetch-local-file
  [source cb]
  (let [uri (.. Image (resolveAssetSource source) -uri)]
    (.. (js/fetch uri)
        (then (fn [blob]
                (.. (.text blob) (then
                                  (fn [content]
                                    (cb content)))))))))

