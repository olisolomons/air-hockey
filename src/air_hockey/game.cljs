(ns air-hockey.game
  (:require
   [air-hockey.sketch-functions :as sf]
   [quil.core :as q]
   [threejs-math :as t]))
(defn ->vec [x y]
  (t/Vector2. x y))
(defn <-vec [v]
  [(.-x v) (.-y v)])
(defn v+ [^t/Vector p1 ^t/Vector p2]
  (.addVectors (t/Vector2.) p1 p2))
(defn v- [^t/Vector p1 ^t/Vector p2]
  (.subVectors (t/Vector2.) p1 p2))
(defn vdiv [^t/Vector p1 divisor]
  (.divideScalar ^t/Vector (.clone p1) divisor))
(defn v* [^t/Vector p1 coefficient]
  (.multiplyScalar ^t/Vector (.clone p1) coefficient))
(defn ->line [^t/Vector p1 ^t/Vector p2]
  {:p p1
   :dir (v- p2 p1)})
(defn intersection [l1 l2]
  (let [pos-diff (v- (:p l1) (:p l2))
        m (t/Matrix3.)
        _ (.set m
                (.-x (:dir l1))
                (.-x (:dir l2))
                0
                (.-y (:dir l1))
                (.-y (:dir l2))
                0
                0 0 1) ]
    (if (zero? (.determinant m))
      nil
      (do (.invert m)
          (.applyMatrix3 pos-diff m)
          {:t1 (- (.-x pos-diff))
           :t2 (.-y pos-diff)
           :p (v+ (v* (:dir l2) (.-y pos-diff)) (:p l2))}))))
(comment
  (def l1 (->line (->vec 0 0) (->vec 2 2)))
  (def l2 (->line (->vec 2 0) (->vec 0 2)))
  (<-vec (:p (intersection l1 l2)))
  (-> (->vec 5 3)
      (v* 2)
      <-vec)
  (js/Object.keys t/Vector2)
  (.-y (v+ (->vec 2 5) (->vec 2 5)))
  (.-y (v- (->vec 2 5) (->vec 2 5)))
  (def m (t/Matrix3.))
  (.set m 2 2 0 2 2 0 0 0 1)
  (.determinant m)
  (.toString (.invert m))
  (def a (js/Array.))
  (.-elements m))

(defn mk-state [size]
  (sf/add-derived-state
   {:canvas-size size
    :puck {:p1 {:pos [50 50]}
           :p2 {:pos [50 (- (second size) 50)]}}
    :type :game}))

(defn- move-puck [puck ds]
  (assoc puck :pos (map + (:pos puck) ds)))
(defn- update-state [state dt]
  (-> state
      (update :puck update-vals
              (fn [{:keys [velocity] :as puck}]
                (cond-> puck
                  velocity (move-puck (map (partial * dt) velocity)))))))

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
