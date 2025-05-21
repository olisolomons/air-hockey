(ns air-hockey.core
  (:require
   [quil.core :as q :include-macros true]
   [quil.middleware :as m]
   [quil.sketch :as sketch]
   [air-hockey.game :as game]
   [air-hockey.sketch-functions :as sf]))

(defn- get-canvas-size []
  [js/window.innerWidth js/window.innerHeight])

(defn prevent-defaults []
  (.addEventListener
   js/document.body
   "keydown"
   (fn [e]
     (when (= (.-keyCode e) 9)
       ;; stop tab key changing focus
       (.preventDefault e)))))

(defn- resize-listener []
  (let [applet (sketch/current-applet)]
    (.addEventListener
     js/window
     "resize"
     (fn [_e]
       (sketch/with-sketch applet
         (let [canvas-size (get-canvas-size)]
           (q/resize-sketch canvas-size canvas-size)
           (swap! (q/state-atom)
                  (comp sf/add-derived-state
                        #(assoc % :canvas-size canvas-size)))
           ;; magic incantation to stop a weird bug
           ;; where the scene appeared super zoomed-in
           ;; sometimes.
           (q/pixel-density (q/display-density))))))))

(defn- touch->clj [rect touch]
  {:id (.-identifier touch)
   :x (- (.-clientX touch) (.-left rect))
   :y (- (.-clientY touch) (.-top rect))})

(defn- add-touch-listener []
  (let [applet (sketch/current-applet)
        canvas (-> js/document (.querySelector ".p5Canvas"))]
    (doseq [[event-name func]
            [["touchstart" sf/touch-start]
             ["touchend" sf/touch-end]
             ["touchmove" sf/touch-move]]]
      (.addEventListener
       canvas
       event-name
       (fn [e]
         (let [touch (aget (.-changedTouches e) 0)
               rect (.getBoundingClientRect (.-target touch))]
           (sketch/with-sketch applet
             (swap! (q/state-atom)
                    (fn [state]
                      (reduce
                       (fn [state touch] (func state (touch->clj rect touch)))
                       state
                       (.-changedTouches e))))))
         (.preventDefault e))
       false))))

(defn setup []
  (add-touch-listener)
  (prevent-defaults)
  (resize-listener)
  (q/frame-rate 60)
  (q/color-mode :rgb)
  (game/mk-state (get-canvas-size)))

(defn draw-state-wrapped [state]
  (q/background 15)
  (sf/draw-state state))

(declare the-sketch)
(defn -main []
  (q/defsketch the-sketch
    :host "app"
    :size (get-canvas-size)
    :setup setup
    :update sf/update-state
    :draw draw-state-wrapped
    :mouse-pressed sf/mouse-pressed
    :mouse-released sf/mouse-released
    :mouse-dragged sf/mouse-dragged
    :key-pressed sf/key-pressed
    :middleware [m/fun-mode]))

(comment
  (sketch/with-sketch the-sketch
    q/state-atom))
