;; NB for difference between `Instant`, `OffsetDateTime`, and `ZonedDateTime`, 
;; see the picture in https://stackoverflow.com/a/32443004/855160

;; some other clojure libraries
;; - http://dm3.github.io/clojure.java-time/
;; - http://clj-time.github.io/clj-time/doc/index.html
(ns fpl.clj.time
  "See also `clojure.instant`."
  (:require
   [fpl.clj.bb :refer [if-bb]])
  (:import
   [java.time Duration Instant ZoneId ZonedDateTime OffsetDateTime]
   java.time.format.DateTimeFormatter))

(defn ms->sec
  [ms]
  (/ ms 1000.0))

(defn epoch-ms
  "Returns ms from epoch."
  []
  (System/currentTimeMillis))

(defn now
  "Returns the current `ZonedDateTime`."
  []
  (ZonedDateTime/now))

;;;
;;; INSTANT
;;;
(defn instant
  "`t` is an int denoting epoch ms, or a string like \"2007-12-03T10:15:30.00Z\"
   ```
   (instant \"2007-12-03T10:15:30.00Z\" :iso8601)
   ```"
  ([]
   (Instant/now))
  ([t]
   (if (int? t)
     (Instant/ofEpochMilli t)
     (Instant/parse t)))
  ([t fmt]
   (case fmt
     ;; rfc1123: "Mon, 3 Dec 2007 10:15:30 GMT"
     :rfc1123 (Instant/from (.parse DateTimeFormatter/RFC_1123_DATE_TIME ^String t))
     ;; iso8601: "2007-12-03T10:15:30Z"
     (:iso :iso8601) (Instant/from (.parse DateTimeFormatter/ISO_INSTANT ^String t)))))

(defn instant->zoned-date-time
  [v]
  (ZonedDateTime/ofInstant v (ZoneId/of "UTC")))

(defn format-instant
  ([v] (format-instant v :iso))
  ([v fmt]
   (case fmt
     :iso (.format DateTimeFormatter/ISO_INSTANT ^Instant v)
     :rfc1123 (.format DateTimeFormatter/RFC_1123_DATE_TIME
                       ^ZonedDateTime (instant->zoned-date-time v)))))

;; no `print-dup` as we don't want to define the reader tag functions.
(defmethod print-method Instant
  [v ^java.io.Writer w]
  (.write w (str "#-inst \"" (format-instant v) "\"")))

(comment
  (println (instant 300))
  (println (str (instant 300))))

;; bb does not support the protocol clojure.core/Inst yet.
;; see issue: https://github.com/babashka/babashka/issues/1321
(if-bb
 nil
 (extend-protocol clojure.core/Inst
   Duration
   (inst-ms* [v] (.toMillis ^Duration v))
   OffsetDateTime
   (inst-ms* [v] (.toEpochMilli (.toInstant ^OffsetDateTime v)))
   ZonedDateTime
   (inst-ms* [v] (.toEpochMilli (.toInstant ^ZonedDateTime v)))))

;;;
;;; ZONED-DATE-TIME
;;;
(defn zoned-date-time
  []
  (ZonedDateTime/now))

(defn format-zoned-date-time
  ([v] (format-zoned-date-time v :iso))
  ([v fmt]
   (case fmt
     :iso (.format DateTimeFormatter/ISO_ZONED_DATE_TIME ^ZonedDateTime v)
     :rfc1123 (.format DateTimeFormatter/RFC_1123_DATE_TIME ^ZonedDateTime v))))

;; no `print-dup` as we don't want to define the reader tag functions.
(defmethod print-method ZonedDateTime
  [v ^java.io.Writer w]
  (.write w (str "#-time \"" (format-zoned-date-time v) "\"")))

(comment
  (print (zoned-date-time)))

(defn today
  "This function gives output such as `20220201`."
  []
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd")
        date (ZonedDateTime/now (ZoneId/of "UTC"))]
    (.format date formatter)))

;;;
;;; DURATION
;;;
(defmethod print-method Duration
  [v ^java.io.Writer w]
  (.write w (str "#-duration \"" (subs (str v) 2) "\"")))

(defn delta
  "Returns a `Duration` from time `t1` to `t2`."
  ^Duration [t1 t2]
  (Duration/between t1 t2))

(defn duration->ms
  ^Long [^Duration d]
  (.toMillis d))

(defn duration->sec
  ^Double [^Duration d]
  (-> d duration->ms ms->sec))

(comment
  (def *t1 (now))
  (def *t2 (now))
  (def *dur (delta *t1 *t2)))
