(ns ui-check.core
  (:require [goog.dom :as gdom]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(def init-data
  {:people [{:id 0 :name "Bob" :friends []}
            {:id 1 :name "Laura" :friends []}
            {:id 2 :name "Mary" :friends []}]})

(defui Friend
  static om/Ident
  (ident [this props]
    [:person/by-id (:id props)])
  static om/IQuery
  (query [this]
    [:id :name]))

(defui Person
  static om/Ident
  (ident [this props]
    [:person/by-id (:id props)])
  static om/IQuery
  (query [this]
    [:id :name {:friends (om/get-query Friend)}]))

(defui People
  static om/IQuery
  (query [this]
    [{:people (om/get-query Person)}]))

(defmulti read om/dispatch)

(defmethod read :people
  [{:keys [state selector] :as env} key _]
  (let [st @state]
    (println selector)
    {:value (om/denormalize selector (get st key) st)}))

(defmulti mutate om/dispatch)

(defn add-friend [state id friend]
  (letfn [(add* [friends ref]
            (cond-> friends
              (not (some #{ref} friends)) (conj ref)))]
    (if-not (= id friend)
      (-> state
        (update-in [:person/by-id id :friends]
          add* [:person/by-id friend])
        (update-in [:person/by-id friend :friends]
          add* [:person/by-id id]))
      friend)))

(defmethod mutate 'friend/add
  [{:keys [state] :as env} key {:keys [id friend] :as params}]
  {:action
   (fn [] (swap! state add-friend id friend))})

(defn remove-friend [state id friend]
  (letfn [(remove* [friends ref]
            (cond->> friends
              (some #{ref} friends) (into [] (remove #{ref}))))]
    (if-not (= id friend)
      (-> state
        (update-in [:person/by-id id :friends]
          remove* [:person/by-id friend])
        (update-in [:person/by-id friend :friends]
          remove* [:person/by-id id]))
      state)))

(defmethod mutate 'friend/remove
  [{:keys [state] :as env} key {:keys [id friend] :as params}]
  {:action (fn [] (swap! state remove-friend id friend))})

(def app-state
  (atom (om/normalize People init-data true)))

(def parser (om/parser {:read read :mutate mutate}))

(def gen-tx-add-remove
  (gen/vector
    (gen/fmap seq
      (gen/tuple
        (gen/elements '[friend/add friend/remove])
        (gen/fmap (fn [[n m]] {:id n :friend m})
          (gen/tuple
            (gen/elements [0 1 2])
            (gen/elements [0 1 2])))))))

(defn self-friended? [{:keys [id friends]}]
  (boolean (some #{id} (map :id friends))))

(defn prop-no-self-friending []
  (prop/for-all [tx gen-tx-add-remove]
    (let [parser (om/parser {:read read :mutate mutate})
          state  (atom (om/normalize People init-data true))]
      (parser {:state state} tx)
      (let [ui (parser {:state state} (om/get-query People))]
        (not (some self-friended? (:people ui)))))))

(defn friends-consistent? [people]
  (let [indexed (zipmap (map :id people) people)]
    (letfn [(consistent? [[id {:keys [friends]}]]
              (let [xs (map (comp :friends indexed :id) friends)]
                (every? #(some #{id} (map :id %)) xs)))]
      (every? consistent? indexed))))

(defn prop-friend-consistency []
  (prop/for-all [tx gen-tx-add-remove]
    (let [parser (om/parser {:read read :mutate mutate})
          state  (atom (om/normalize People init-data true))]
      (parser {:state state} tx)
      (let [ui (parser {:state state} (om/get-query People))]
        (friends-consistent? (:people ui))))))

(comment
  (gen/sample gen-tx-add 10)

  (tc/quick-check 100 (prop-no-self-friending))
  (tc/quick-check 100 (prop-friend-consistency))

  (om/normalize People init-data true)

  ;; basic testing
  (-> (parser {:state app-state} (om/get-query People))
    :people (nth 0) self-friended?)

  (parser {:state app-state} (om/get-query People))
  (parser {:state app-state} '[(friend/add {:id 0 :friend 1})])
  (parser {:state app-state} '[(friend/add {:id 1 :friend 1})])
  (parser {:state app-state} '[(friend/remove {:id 1 :friend 1})])

  (some self-friended?
    (:people (parser {:state app-state} (om/get-query People))))

  (friends-consistent?
    (:people (parser {:state app-state} (om/get-query People))))
  )