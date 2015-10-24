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
  static om/Query
  (query [this]
    [{:people (om/get-query Person)}]))

(comment


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