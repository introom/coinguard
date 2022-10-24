(ns app.api.account-test
  (:require
   [app.api.account :as account]
   [malli.core :as m]
   [malli.transform :as mt]
   [clojure.test :as t]))

(t/deftest validate-create-user-input
  (t/testing "Valid inputs"
    (t/is (m/validate account/s:create-account
                      {:email "free@solo.com"
                       :password "some-length"})))
  (t/testing "Invalid inputs"
    (t/is (false? (m/validate account/s:create-account
                              {:email "free@solo.com"}))
          "Missing password")))

(comment
  (-> (m/explain account/s:create-account
                 {"email" "kkkkkkfree@solo.com"
                  :password "3asdeeeeef"})
      #_(me/humanize))

  (m/decode account/s:create-account
            {"xemail" "kkkkkkfree@solo.com"
             :password "3asdeeeeef"}
            mt/json-transformer))