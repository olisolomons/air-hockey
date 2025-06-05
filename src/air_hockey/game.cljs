(ns air-hockey.game
  (:require
   [air-hockey.sketch-functions :as sf]
   [quil.core :as q]
   [threejs-math :as t]))

(extend-protocol IPrintWithWriter
  t/Vector2
  (-pr-writer [obj writer opts]
    (doseq [part ["(->vec " (.-x obj) " " (.-y obj) ")"]]
      (-write writer part))))

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
  (let [dir (v- p2 p1)]
    {:p1 p1
     :p2 p2
     :dir dir
     :normal (-> dir
                 .clone
                 (.rotateAround
                  (->vec 0 0)
                  (/ js/Math.PI 2))
                 .normalize)}))

(defn- point-on-line [l2 t2]
  (-> l2
      :dir
      (v* t2)
      (v+ (:p1 l2))))

(defn intersection [l1 l2]
  (let [pos-diff (v- (:p1 l1) (:p2 l2))
        m (t/Matrix3.)
        _ (.set m
                (.-x (:dir l1))
                (.-x (:dir l2))
                0
                (.-y (:dir l1))
                (.-y (:dir l2))
                0
                0 0 1)]
    (if (zero? (.determinant m))
      nil
      (let [_ (.invert m)
            _ (.applyMatrix3 pos-diff m)
            t1 (- (.-x pos-diff))
            t2 (- (.-y pos-diff))]
        (def t1 t1)
        (def t2 t2)
        (when (and (< 0 t1 1) (< 0 t2 1))
          {:t1 t1
           :t2 t2
           :l1 l1
           :l2 l2})))))
(defn not-zero [number]
  (when-not (zero? number) number))

(defn intersection-offset [offset l1 l2]
  (when-let [qwe (not-zero ^js/Number (.dot (:dir l1) (:normal l2)))]
    (let [t-offset (js/Math.abs (/ ^js/Number offset ^js/Number qwe))
          t2-offset (js/Math.sqrt (- (js/Math.pow (* t-offset (.length (:dir l1))) 2)
                                     (* offset offset)))
          _ (println t2-offset)
          pos-diff (v- (:p1 l1) (:p2 l2))
          m (t/Matrix3.)
          _ (.set m
                  (.-x (:dir l1))
                  (.-x (:dir l2))
                  0
                  (.-y (:dir l1))
                  (.-y (:dir l2))
                  0
                  0 0 1)
          _ (.invert m)
          _ (.applyMatrix3 pos-diff m)
          t1 (- (- (.-x pos-diff))
                t-offset)
          t2 (- (.-y pos-diff))]
      (when (and (< 0 t1 1) (< (- t2-offset) t2 (+ 1 t2-offset)))
        {:t1 t1
         :t2 t2
         :l1 l1
         :l2 l2}))))
(comment
  (def l1 (->line (->vec 0 0) (->vec 2 2)))
  (def l2 (->line (->vec 2 0) (->vec 0 2)))
  (intersection l1 l2)
  (point-on-line l2 0.5)
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

(defn mk-state [[width height :as size]]
  (sf/add-derived-state
   {:canvas-size size
    :puck {:p1 {:pos (->vec 50 50)}
           :p2 {:pos (->vec 50 (- height 50))}}
    :lines [(->line (->vec 0 0) (->vec 0 height))
            (->line (->vec 0 0) (->vec width 0))
            (->line (->vec 0 height) (->vec width height))
            (->line (->vec width 0) (->vec width height))]

    :type :game}))

(defn reflect [v ^t/Vector2 wall-normal]
  (let [d (.dot wall-normal v)]
    (v+ v (v* wall-normal (* d -2)))))

(def puck-radius 20)

(defn- move-puck [puck dt ds lines]
  (let [pos (:pos puck)
        velocity (:velocity puck)
        move {:p1 pos
              :dir ds}
        first-hit (->> lines
                       (keep (partial intersection-offset puck-radius move))
                       (reduce
                          (fn [a b]
                            (if (< (:t1 b) (:t1 a))
                              b a))))
        new-pos (point-on-line move (or (:t1 first-hit) 1))
        puck (assoc puck :pos new-pos)]
    (if first-hit
      (let [puck (assoc puck :velocity
                        (v* (reflect velocity (:normal (:l2 first-hit)))
                            0.95))
            new-ds (v* (reflect ds (:normal (:l2 first-hit))) (- 1 (:t1 first-hit)))]
        (recur puck
              dt
              (v* new-ds (- 1 (:t1 first-hit))) lines))
      (update puck
              :velocity
              v* (js/Math.pow 0.9 (/ 1 dt))))))

(defn- update-state [state dt]
  (-> state
      (update :puck update-vals
              (fn [{:keys [velocity] :as puck}]
                (cond-> puck
                  velocity (move-puck dt (v* velocity dt) (:lines state)))))))

(defmethod sf/update-state :game
  [{:keys [previous-time] :as state}]
  (let [now (q/millis)
        dt (- now previous-time)]
    (assoc
     (if previous-time
       (update-state state dt)
       state)
     :previous-time now)))

(defn draw-circle [x y radius]
  (q/ellipse x y (* 2 radius) (* 2 radius)))
(defmethod sf/draw-state :game
  [{:keys [puck lines] [width height] :canvas-size}]
  (q/no-stroke)
  (q/fill 50 0 0)
  (q/rect 0 0 width (/ height 2))
  (q/fill 255 50 50)
  (q/rect 0 (/ height 2) width 0)
  (doseq [[_player {:keys [pos]}] puck
          :let [[x y] (<-vec pos)]]
    (draw-circle x y puck-radius))
  (q/stroke 255 255 255)
  (doseq [{:keys [p1 p2]} lines]
    (def p1 p1)
    (q/line (<-vec p1) (<-vec p2))))

(defmethod sf/add-derived-state :game
  [state]
  state)

(defmethod sf/touch-start :game
  [state {:keys [x y id]}]
  (if-let [player
           (some
            (fn [[player {:keys [pos]}]]
              (when (< (.distanceTo ^t/Vector2 pos (->vec x y)) puck-radius)
                player))
            (:puck state))]
    (-> state
        (assoc-in [:dragging id] {:player player :history #queue []})
        (update-in [:puck player] dissoc :velocity))
    state))

(defn- conj-limit [queue element limit]
  (cond-> queue
    (>= (count queue) limit) pop
    :always (conj element)))

(defmethod sf/touch-move :game
  [{:keys [dragging] :as state} {:keys [id x y]}]
  (if-let [{:keys [player]} (get dragging id)]
    (do
      (def state state)
      (def player player)
      (def x x)
      (def y y)
      (def id id)
      (def ms (q/millis))
      (-> state
          (assoc-in [:puck player :pos] (->vec x y))
          (update-in [:dragging id :history]
                     conj-limit [x y (q/millis)] 5)))
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
                              (-> puck
                                  :pos
                                  (v- (->vec x y))
                                  (vdiv dt)))))))
    state))
