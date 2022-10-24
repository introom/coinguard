(ns app.util.time
  (:import
   java.time.Duration))

;; duration
(defn- dict->duration
  [{:keys [days hours minutes seconds millis nanos]}]
  (cond-> (Duration/ofMillis (if (int? millis) ^long millis 0))
    (int? days)    (.plusDays ^long days)
    (int? hours)   (.plusHours ^long hours)
    (int? minutes) (.plusMinutes ^long minutes)
    (int? seconds) (.plusSeconds ^long seconds)
    (int? nanos)   (.plusNanos ^long nanos)))

(defn duration?
  [v]
  (instance? Duration v))

(defn duration
  ^Duration [ms-or-dict]
  (cond
    (string? ms-or-dict)
    (Duration/parse (str "PT" ms-or-dict))

    (integer? ms-or-dict)
    (Duration/ofMillis ms-or-dict)

    (duration? ms-or-dict)
    ms-or-dict

    :else
    (dict->duration ms-or-dict)))

(comment)