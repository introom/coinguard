(ns app.alert
  (:require
   [app.logging :as log]
   [integrant.core :as ig]
   [app.config :as config])
  (:import (com.twilio Twilio)
           (com.twilio.rest.api.v2010.account Call Message)
           (com.twilio.type PhoneNumber Twiml)))

(defn- send-message
  [from-account to-account content]
  (-> (Message/creator (PhoneNumber. to-account)
                       (PhoneNumber. from-account)
                       content)
      .create
      .getSid))

(defn- make-phone-call
  [from-account to-account content]
  (-> (Call/creator (PhoneNumber. to-account)
                    (PhoneNumber. from-account)
                    (Twiml. (format "<Response>
                                     <Say>%s</Say>
                                     <Pause length=\"2\"/>
                                     <Say>Bye bye</Say>
                                     </Response>" content)))
      .create
      .getSid))

(defn create-mobile-task-handler
  [& {:keys [db] :as ctx}]
  (let [account-sid (config/get :c/twilio-account-sid)
        auth-token (config/get :c/twilio-auth-token)
        from-account (config/get :c/twilio-from-account)]
    (Twilio/init account-sid auth-token)
    (fn [{{:keys [type to-account content]} :props
          :as task}]
      (log/info "mobile handler invoked" :task task)
      (case type
        "phone" (make-phone-call from-account to-account content)
        "sms" (send-message from-account to-account content)))))

(comment
  (require 'app.main)
  (def *system (ig/init app.main/system-config [::mobile-task-handler]))
  (def *handler (::mobile-task-handler *system))
  (*handler {:props {:type "phone"
                     :to-account "+19896970093"
                     :content "block alert"}})

  (*handler {:props {:type "sms"
                     :to-account "+19896970093"
                     :content "block alert"}}))
