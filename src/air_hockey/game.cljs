(ns air-hockey.game
  (:require
   [air-hockey.sketch-functions :as sf]
   [quil.core :as q]))

(defn mk-state [size]
  (sf/add-derived-state
   {:canvas-size size
    :puck {:p1 {:pos [50 50]}
           :p2 {:pos [50 (- (second size) 50)]}}
    :type :game}))

(defn- update-state [state dt]
  (-> state
      (update :puck update-vals
              (fn [{:keys [velocity] :as puck}]
                (cond-> puck
                  velocity (update :pos (partial map + (map (partial * dt) velocity))))))))

(defmethod sf/update-state :game
  [{:keys [previous-time] :as state}]
  (let [now (q/millis)
        dt (- now previous-time)]
    (assoc
     (if previous-time
       (update-state state dt)
       state)
     :previous-time now)))

(def puck-radius 20)

(defn draw-circle [x y radius]
  (q/ellipse x y (* 2 radius) (* 2 radius)))

(defmethod sf/draw-state :game
  [{:keys [puck] [width height] :canvas-size}]
  (q/fill 50 0 0)
  (q/rect 0 0 width (/ height 2))
  (q/fill 255 50 50)
  (q/rect 0 (/ height 2) width 0)
  (doseq [[_player {[x y] :pos}] puck]
    (draw-circle x y puck-radius)))

(defmethod sf/add-derived-state :game
  [state]
  state)

(defn in-dist? [p1 p2 limit]
  (let [[dx dy] (map - p2 p1)]
    (< (+ (* dx dx) (* dy dy))
       (* limit limit))))

(defmethod sf/touch-start :game
  [state {:keys [x y id]}]
  (if-let [player
           (some
            (fn [[player {:keys [pos]}]]
              (when (in-dist? pos [x y] puck-radius)
                player))
            (:puck state))]
    (assoc-in state [:dragging id] {:player player :history #queue []})
    state))

(defn- conj-limit [queue element limit]
  (cond-> queue
    (>= (count queue) limit) pop
    :always (conj element)))

(defmethod sf/touch-move :game
  [{:keys [dragging] :as state} {:keys [id x y]}]
  (if-let [{:keys [player]} (get dragging id)]
    (-> state
        (assoc-in [:puck player :pos] [x y])
        (update-in [:dragging id :history]
                   conj-limit [x y (q/millis)] 20))
    state))

(defmethod sf/touch-end :game
  [{:keys [dragging] :as state} {:keys [id]}]
  (if-let [{:keys [player history]} (get dragging id)]
    (-> state
        (update :dragging dissoc id)
        (update-in [:puck player]
                   (fn [puck]
                     (let [[x y t] (peek history)
                           dt (- (q/millis) t)]
                       (assoc puck :velocity
                              (map #(/ % dt)
                                   (map - (:pos puck) [x y])))))))
    state))
