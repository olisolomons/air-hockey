(ns cljs-cloudflare-discord.core)

(defn ^:export handler [request _env _ctx]
  (str "hello! url is " (.-url request)))
