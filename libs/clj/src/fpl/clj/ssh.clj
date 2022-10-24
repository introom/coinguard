(ns fpl.clj.ssh
  (:require
   [fpl.clj.template :as tmpl]
   [fpl.clj.process :refer [sh]]))

(def ^:dynamic *ssh-opts*
  {:username nil
   :host nil
   :port 22
   :identities-only "yes"
   :identity-file nil
   :ssh-opts ""})

(def default-proc-opts {:max-cmd-length 30})

(defn- ssh-assert
  []
  (let [opts (select-keys *ssh-opts* [:username :host])]
    (assert (->> opts vals (filter nil?) empty?) opts)))

(defmacro with-ssh
  [opts & body]
  `(binding [*ssh-opts* (merge *ssh-opts* ~opts)]
     (ssh-assert)
     ~@body))
(comment
  (macroexpand-1 '(with-ssh {:username "foo" :host "localhost"})))

(defn ssh-session-cmd
  []
  (-> (str "ssh {{username}}@{{host}}"
           " -p {{port}}"
           " -o IdentitiesOnly={{identities-only}}"
           " -o ControlMaster=auto"
           " -o ControlPath=/tmp/ssh-%r@%h-%p.sock"
           "{% if identity-file %}"
           " -o IdentityFile={{identity-file}}"
           "{% endif %}"
           " -o ControlPersist=1800"
           " -o ForwardAgent=no"
           " -o StrictHostKeyChecking=no"
           ;; do not add the fingerprint to `known_hosts`.
           " -o UserKnownHostsFile=/dev/null"
           "{% if ssh-opts|not-empty %}"
           " {{ssh-opts}}"
           "{% endif %}")
      (tmpl/render *ssh-opts*)))

(comment
  (with-ssh {:username "john" :host "example.com"}
    (ssh-session-cmd)))

(defn- ssh-remote-cmd
  [remote-cmd]
  ;; no need to 
  remote-cmd)

(defn- ssh-cmd
  [remote-cmd]
  (str (ssh-session-cmd) " " (ssh-remote-cmd remote-cmd)))

(defn shell
  ([cmd]
   (shell nil))
  ([cmd proc-opts]
   (let [cmd (ssh-cmd cmd)]
     (sh cmd (merge default-proc-opts proc-opts)))))

(comment
  (with-ssh {:username "firepanda" :host "195.201.121.3"}
    (shell "ls -al")))

(defn- scp-cmd
  [local-path remote-path direction]
  (-> (str "scp"
           " -P {{port}}"
           " -o IdentitiesOnly={{identities-only}}"
           " -o ControlMaster=auto"
           " -o ControlPath=/tmp/ssh-%r@%h-%p.sock"
           "{% if identity-file %}"
           " -o IdentityFile={{identity-file}}"
           "{% endif %}"
           " -o ControlPersist=1800"
           " -o StrictHostKeyChecking=no"
           ;; do not add the fingerprint to `known_hosts`.
           " -o UserKnownHostsFile=/dev/null"
           "{% if ssh-opts|not-empty %}"
           " {{ssh-opts}}"
           "{% endif %}"
           "{% ifequal direction :up %}"
           " {{local-path}} {{username}}@{{host}}:{{remote-path}}"
           "{% else %}"
           " {{username}}@{{host}}:{{remote-path}} {{local-path}}"
           "{% endifequal %}")
      (tmpl/render (assoc *ssh-opts* :local-path local-path :remote-path remote-path :direction direction))))

(defn scp
  ([local-path remote-path]
   (scp local-path remote-path :up nil))
  ([local-path remote-path direction]
   (scp local-path remote-path :up nil))
  ([local-path remote-path direction proc-opts]
   {:pre [(#{:up :down} direction)]}
   (sh (scp-cmd local-path remote-path direction) (merge default-proc-opts proc-opts))))

(comment
  (with-ssh {:username "firepanda" :host "195.201.121.3"}
    (scp-cmd "README.md" "./bar" :up))
  (with-ssh {:username "firepanda" :host "195.201.121.3"}
    (scp "README.md" "./bar" :up)))

(defn session
  ([]
   (session nil))
  ([proc-opts]
   (sh (ssh-session-cmd) (merge default-proc-opts proc-opts))))

(defn script
  ([content]
   (script content nil))
  ([content proc-opts]
   (shell "bash -s" (merge default-proc-opts {:in content}))))