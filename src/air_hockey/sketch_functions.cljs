(ns air-hockey.sketch-functions)

(defmulti mouse-dragged :type)
(defmethod mouse-dragged :default
 [state _event]
 state)

(defmulti update-state :type)
(defmethod update-state :default
  [state]
  state)

(defmulti draw-state :type)
(defmethod draw-state :default
  [_state])

(defmulti mouse-pressed :type)
(defmethod mouse-pressed :default
  [state _event]
  state)

(defmulti touch-end :type)
(defmethod touch-end :default
  [state _event]
  state)

(defmulti touch-move :type)
(defmethod touch-move :default
  [state _event]
  state)

(defmulti touch-start :type)
(defmethod touch-start :default
  [state _event]
  state)

(defmulti mouse-released :type)
(defmethod mouse-released :default
  [state _event]
  state)

(defmulti key-pressed :type)
(defmethod key-pressed :default
  [state _event]
  state)

(defmulti add-derived-state :type)
(defmethod add-derived-state :default
  [state]
  state)


