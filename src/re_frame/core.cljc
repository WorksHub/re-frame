(ns re-frame.core
  (:require
    [re-frame.events      :as events]
    [re-frame.subs        :as subs]
    [re-frame.fx          :as fx]
    [re-frame.cofx        :as cofx]
    [re-frame.router      :as router]
    [re-frame.loggers     :as loggers]
    [re-frame.registrar   :as registrar]
    [re-frame.interceptor :as interceptor]
    [re-frame.std-interceptors :as std-interceptors :refer [db-handler->interceptor
                                                             fx-handler->interceptor
                                                             ctx-handler->interceptor]]))


;; --  dispatch
(def dispatch         router/dispatch)
(def dispatch-sync    router/dispatch-sync)


;; XXX move certain API functions up to this core level - to get code completion and docs
;; XXX add a clear all handlers
;; XXX for testing purposes, and a way to snapshot re-frame instance. Then re-instate
;; XXX on figwheel reload, should invalidate all re-frame subscriptions


;; -- interceptor related
;; useful if you are writing your own interceptors
(def ->interceptor   interceptor/->interceptor)
(def enqueue         interceptor/enqueue)
(def get-coeffect    interceptor/get-coeffect)
(def get-effect      interceptor/get-effect)
(def assoc-effect    interceptor/assoc-effect)
(def assoc-coeffect  interceptor/assoc-coeffect)


;; --  standard interceptors
(def debug       std-interceptors/debug)
(def path        std-interceptors/path)
(def enrich      std-interceptors/enrich)
(def trim-v      std-interceptors/trim-v)
(def after       std-interceptors/after)
(def on-changes  std-interceptors/on-changes)


;; --  subscriptions: reading and writing
(def reg-sub-raw         subs/register-raw)
(def reg-sub             subs/reg-sub)
(def subscribe           subs/subscribe)

;; -- effects
(def reg-fx       fx/register)
(def clear-fx     (partial registrar/clear-handlers fx/kind))

;; -- coeffects
(def reg-cofx       cofx/register)
(def clear-cofx     (partial registrar/clear-handlers cofx/kind))

;; --  Events

;; usage (clear-event! :some-id)
(def clear-all-events!  (partial registrar/clear-handlers events/kind))    ;; XXX name with !


(defn reg-event-db
  "Register the given `id`, typically a keyword, with the combination of
  `db-handler` and an interceptor chain.
  `db-handler` is a function: (db event) -> db
  `interceptors` is a collection of interceptors, possibly nested (needs flattenting).
  `db-handler` is wrapped in an interceptor and added to the end of the chain, so in the end
   there is only a chain.
   The necessary effects and coeffects handler are added to the front of the
   interceptor chain.  These interceptors ensure that app-db is available and updated."
  ([id db-handler]
    (reg-event-db id nil db-handler))
  ([id interceptors db-handler]
   (events/register id [cofx/add-db fx/do-effects interceptors (db-handler->interceptor db-handler)])))


(defn reg-event-fx
  ([id fx-handler]
   (reg-event-fx id nil fx-handler))
  ([id interceptors fx-handler]
   (events/register id [cofx/add-db fx/do-effects interceptors (fx-handler->interceptor fx-handler)])))


(defn reg-event-ctx
  ([id handler]
   (reg-event-ctx id nil handler))
  ([id interceptors handler]
   (events/register id [cofx/add-db fx/do-effects interceptors (ctx-handler->interceptor handler)])))


;; --  Logging -----
;; Internally, re-frame uses the logging functions: warn, log, error, group and groupEnd
;; By default, these functions map directly to the js/console implementations,
;; but you can override with your own fns (set or subset).
;; Example Usage:
;;   (defn my-fn [& args]  (post-it-somewhere (apply str args)))  ;; here is my alternative
;;   (re-frame.core/set-loggers!  {:warn my-fn :log my-fn})       ;; override the defaults with mine
(def set-loggers! loggers/set-loggers!)

;; If you are writing an extension to re-frame, like perhaps
;; an effeects handler, you may want to use re-frame logging.
;;
;; usage:  (console :error "this is bad: " a-variable " and " anotherv)
;;         (console :warn "possible breach of containment wall at: " dt)
(def console loggers/console)


;; -- Registrar
;; XXX
;; In testing you often need to store the current state of the registrar
;; and then reinstate it  state, and then reinstate
;; that state at the end of the
;; then re-instate it.
;; So, in a testing scenario, you'd store the
;; current state,  make changes and then put back the way it was.
;; OR should this be done with bindings ??


;; -- Event Procssing Callbacks

(defn add-post-event-callback
  "Registers a function `f` to be called after each event is procecessed
   `f` will be called with two arguments:
    - `event`: a vector. The event just processed.
    - `queue`: a PersistentQueue, possibly empty, of events yet to be processed.

   This is useful in advanced cases like:
     - you are implementing a complex bootstrap pipeline
     - you want to create your own handling infrastructure, with perhaps multiple
       handlers for the one event, etc.  Hook in here.
     - libraries providing 'isomorphic javascript' rendering on  Nodejs or Nashorn.

  'id' is typically a keyword. Supplied at \"add time\" so it can subsequently
  be used at \"remove time\" to get rid of the right callback.
  "
  ([f]
   (add-post-event-callback f f))   ;; use f as its own identifier
  ([id f]
   (router/add-post-event-callback re-frame.router/event-queue id f)))


(defn remove-post-event-callback
  [id]
  (router/remove-post-event-callback re-frame.router/event-queue id))


;; --  Deprecation Messages
;; Assisting the v0.0.7 ->  v0.0.8 tranistion.
(defn register-handler
  [& args]
  (console :warn  "re-frame:  \"register-handler\" has been renamed \"reg-event-db\" (look for registration of " (str (first args)) ")")
  (apply reg-event-db args))

(defn reg-event
  [& args]
  (console :warn  "re-frame:  \"reg-event\" has been renamed \"reg-event-db\" (look for registration of " (str (first args)) ")")
  (apply reg-event-db args))

(defn register-sub
  [& args]
  (console :warm  "re-frame:  \"register-sub\" is deprecated. Use \"reg-sub-raw\" (look for registration of " (str (first args)) ")")
  (apply reg-sub-raw args))

