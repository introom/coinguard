(ns app.code)

;; general codes
(def succeeded :succeeded)
(def failed :failed)
(def authentication-failed :authentication-failed)
(def access-denied :access-denied)
(def internal-error :internal-error)

;; db codes
(def db:timeout :db/timeout)
(def db:resource-already-exists :db/resource-already-exists)
(def db:psql-error :db/psql-error)