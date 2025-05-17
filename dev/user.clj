(ns user
  (:require
    [shadow.cljs.devtools.api :as shadow]
    [shadow.cljs.devtools.server :as server]))

(defn cljs []
  (server/start!)
  (shadow/watch :app)
  (shadow/repl :app))

(comment
  (cljs))
