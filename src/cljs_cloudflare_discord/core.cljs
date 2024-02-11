(ns cljs-cloudflare-discord.core
  (:require
   ["discord-interactions" :as di]
   [clojure.set :as set]))

(defn enum->map [enum] (zipmap (map keyword (js/Object.keys enum)) (js/Object.values enum)))
(def interaction-types (set/map-invert (enum->map di/InteractionType)))
(def interaction-responses (enum->map di/InteractionResponseType))

(defn valid-discord-request? [request body env]
  (let [signature (-> request .-headers (.get "x-signature-ed25519"))
        timestamp (-> request .-headers (.get "x-signature-timestamp"))
        public-key (.-DISCORD_PUBLIC_KEY ^js env)]
    (when-not public-key (throw (ex-info "no public key env var" {})))
    (and signature
         timestamp
         (di/verifyKey body signature timestamp public-key))))

(defn handle-discord-request [request interaction _env]
  (case (->> interaction :type (get interaction-types))
    :PING {:type :PONG}))

(defn json-response [body opts]
  (js/Response. (js/JSON.stringify (clj->js body))
                (clj->js (assoc-in opts
                                   [:headers "content-type"]
                                   "application/json"))))

(defn ^:export handler [request env _ctx]
  (case [(.-pathname (js/URL. (.-url request)))
         (keyword (.-method request))]
    ["/" :GET] (js/Response. "Hello, I'm a discord app!")
    ["/" :POST] (-> (.text request)
                    (.then (fn [body]
                             (if (and body (valid-discord-request? request body env))
                               (if-let [response (handle-discord-request request
                                                                         (js->clj (js/JSON.parse body) :keywordize-keys true)
                                                                         env)]
                                 (-> response
                                     (update :type interaction-responses)
                                     (json-response {:status 200}))
                                 (json-response {:error "Unknown type"} {:status 400}))
                               (js/Response. "Bad request signature" #js {:status 401})))))
    (js/Response. "Not found" #js {:status 404})))
