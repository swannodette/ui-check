(ns ui-check.core
  (:require [goog.dom :as gdom]
            [clojure.test.check.generators :as gen]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(def init-data
  {:people [{:id 0 :name "Bob" :friends []}
            {:id 1 :name "Laura" :friends []}
            {:id 2 :name "Mary" :friends []}]})

(defui Person
  static om/Ident
  (ident [this props]
    [:person/by-id (:id props)])
  static om/IQuery
  (query [this]
    [:id :name :friends]))

(defui People
  static om/IQuery
  (query [this]
    [{:people (om/get-query Person)}]))

(defmulti read om/dispatch)

(defmethod read :people
  [{:keys [state]} key params]
  (let [st @state]
    {:value (into [] (map #(get-in st %)) (:people st))}))

(defmulti mutate om/dispatch)

(defn add-friend [friends ref]
  (cond-> friends
    (not (some #{ref} friends)) (conj ref)))

(defmethod mutate 'friend/add
  [{:keys [state] :as env} key {:keys [id friend] :as params}]
  {:action
   (fn []
     (swap! state
       (fn [state']
         (-> state'
           (update-in [:person/by-id id :friends]
             add-friend [:person/by-id friend])
           (update-in [:person/by-id friend :friends]
             add-friend [:person/by-id id])))))})

(defn remove-friend [friends ref]
  (cond->> friends
    (some #{ref} friends) (into [] (remove #{ref}))))

(defmethod mutate 'friend/remove
  [{:keys [state] :as env} key {:keys [id friend] :as params}]
  {:action
   (fn []
     (swap! state
       (fn [state']
         (-> state'
           (update-in [:person/by-id id :friends]
             remove-friend [:person/by-id friend])
           (update-in [:person/by-id friend :friends]
             remove-friend [:person/by-id id])))))})

(def app-state
  (atom (om/normalize People init-data true)))

(def parser (om/parser {:read read :mutate mutate}))

(comment
  (parser {:state app-state} [:people])

  (parser {:state app-state} '[(friend/add {:id 0 :friend 1})])
  @app-state
  (parser {:state app-state} '[(friend/remove {:id 0 :friend 1})])
  @app-state

  (def init-state
    {:ids #{}})

  (defn gen-event
    [{:keys [ids]}]
    (let [gen-create (gen/fmap #(list 'todo/create {:id %})
                       (gen/such-that #(not (ids %))
                         gen/nat))]
      (if (empty? ids)
        gen-create
        (let [gen-update (gen/fmap #(list 'todo/update %)
                           (gen/elements ids))]
          (gen/frequency [[3 gen-update]
                          [1 gen-create]])))))

  (defn update-state
    [state ev]
    (if (= 'todo/create (first ev))
      (update state :ids conj (:id (second ev)))
      state))

  (def gen-events
    (gen/bind gen/nat
      (partial
        (fn self [state ev-count]
          (if (zero? ev-count)
            (gen/return ())
            (gen/bind (gen-event state)
              (fn [event]
                (gen/fmap #(cons event %)
                  (-> state
                    (update-state event)
                    (self (dec ev-count))))))))
        init-state)))
  (gen/sample gen-events 10)

  )