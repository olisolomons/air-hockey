(ns register-commands
  (:require
   [clj-http.client :as http]
   [jsonista.core :as j]))

(defn getenv [var-name]
  (or (System/getenv var-name)
      (throw (ex-info (str "required env var not provided: " var-name) {:var-name var-name}))))

(def token (getenv "DISCORD_TOKEN"))

(def application-id (getenv "DISCORD_APPLICATION_ID"))

(def url (str "https://discord.com/api/v10/applications/" application-id "/commands"))

(defn ^:export register [& _args]
  (let [response
        (http/put url
                  {:headers {"Content-Type" "application/json"
                             "Authorization" (str "Bot " token)}
                   :body (j/write-value-as-string [])})]
    (if (= (:status response) 200)
      (do (println "Registered all commands")
          (prn (j/read-value (:body response))))
      (do (println "Error registering commands")
          (prn (select-keys response [:status :reason-phrase :body]))))))
