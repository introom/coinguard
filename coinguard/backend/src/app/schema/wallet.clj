(ns app.schema.wallet)

(def s:condition-percent
  [:map {:cosed true}
   [:type [:= :percent]]
   [:value string?]])

(def s:condition-amount
  [:map {:closed true}
   [:type [:= :amount]]
   [:value string?]])

(def s:alert
  [:map {:closed true}
   [:name string?]
   [:conditions [:vector {:gen/min 1 :gen/max 2}
                 [:or s:condition-percent s:condition-amount]]]])

(def s:data
  [:map {:closed true}
   [:name string?]
   [:coin string?]
   [:address string?]
   [:alerts [:vector {:gen/min 1 :gen/max 2} s:alert]]])

(def s:wallet
  [:map {:closed true}
   [:id uuid?]
   [:account-id uuid?]
   [:data s:data]])