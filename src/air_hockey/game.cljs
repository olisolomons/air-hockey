(ns air-hockey.game
  (:require
    [air-hockey.sketch-functions :as sf]
    [quil.core :as q]))

(defn mk-state [size]
  (sf/add-derived-state
    {:canvas-size size
     :type :game}))

(defmethod sf/draw-state :game
  [{[width height] :canvas-size}]
  (q/fill 0 50 50)
  (q/rect 0 0 width (/ height 2))
  (q/fill 255 50 50)
  (q/rect 0 (/ height 2) width 0)
  (q/ellipse 56 46 55 55))

(defmethod sf/add-derived-state :game
  [state]
  state)
