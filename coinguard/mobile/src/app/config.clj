(ns app.config)

(defmacro get-config-file-name
  []
  (or
   (System/getProperty "app.configFile")
   (System/getenv "APP_CONFIG_FILE")
   "config/qa.json"))

(comment
  (get-config-file-name))