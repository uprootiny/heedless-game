(ns heedless-game.core
  (:require [goog.dom :as gdom]
            [goog.events :as events]
            [clojure.string :as str]
            [heedless-game.scenarios :refer [scenarios]]
            [heedless-game.scoring   :as scoring]))

;; -----------------------------------------------------------------------------
;; Single atom holds the entire game state.
;;
;;   :phase     :intro | :playing | :resolving | :finished
;;   :index     current scenario index (0..N-1)
;;   :chosen    vector of chosen choice maps so far
;;   :current-choice the most-recently-clicked choice (during :resolving)
;; -----------------------------------------------------------------------------

(def initial-state
  {:phase  :intro
   :index  0
   :chosen []
   :current-choice nil})

(defonce state (atom initial-state))

(declare start-game! choose! advance! restart! scroll-top!)

;; -----------------------------------------------------------------------------
;; DOM helpers — hand-rolled so we don't pull in React for a seven-page game.
;;
;; `h` builds an Element from a tag, props map, and varargs children.
;; Children may be strings, numbers, Elements, or nested seqs.
;; -----------------------------------------------------------------------------

(defn- set-attrs! [el props]
  (doseq [[k v] props]
    (cond
      (= k :on-click)  (events/listen el "click" v)
      (= k :class)     (.setAttribute el "class" v)
      (= k :style)     (set! (.. el -style -cssText) v)
      (= k :html)      (set! (.-innerHTML el) v)
      (= k :data-key)  (.setAttribute el "data-key" v)
      :else            (.setAttribute el (name k) (str v)))))

(defn- append! [el child]
  (cond
    (nil? child) nil
    (sequential? child) (doseq [c child] (append! el c))
    (string? child)  (.appendChild el (.createTextNode js/document child))
    (number? child)  (.appendChild el (.createTextNode js/document (str child)))
    :else            (.appendChild el child)))

(defn h
  ([tag] (h tag {}))
  ([tag props & children]
   (let [el (.createElement js/document (name tag))]
     (set-attrs! el (or props {}))
     (doseq [c children] (append! el c))
     el)))

;; -----------------------------------------------------------------------------
;; Markdown-light: ``code`` and **bold** inline rendering for setup text.
;; -----------------------------------------------------------------------------

(defn- inline-md [text]
  ;; Convert ``code`` → <code>code</code> and **bold** → <strong>bold</strong>.
  ;; Preserves whitespace and newlines.
  (let [escape (fn [s]
                 (-> s
                     (str/replace "&" "&amp;")
                     (str/replace "<" "&lt;")
                     (str/replace ">" "&gt;")))
        s (escape text)
        s (str/replace s #"```([\s\S]*?)```" "<pre><code>$1</code></pre>")
        s (str/replace s #"`([^`]+)`"        "<code>$1</code>")
        s (str/replace s #"\*\*([^*]+)\*\*"   "<strong>$1</strong>")
        s (str/replace s "\n\n" "</p><p>")
        s (str "<p>" s "</p>")]
    s))

;; -----------------------------------------------------------------------------
;; Views — pure functions of state, return Elements.
;; -----------------------------------------------------------------------------

(defn- view-intro []
  (h :div {:class "screen screen-intro"}
     (h :p {:class "eyebrow"} "a small choose-your-own-adventure")
     (h :h1 nil "What kind of hand are you?")
     (h :p {:class "lede"}
        "Seven scenarios. Each one tests a principle from the "
        (h :a {:href "https://uprootiny.github.io/heedless-manifesto/" :target "_blank" :rel "noopener"} "manifesto")
        ". Some choices feel costly and are correct. Some feel correct and are costly. Choose without hedging.")
     (h :button {:class "primary cta-large" :on-click start-game!} "Begin")
     (h :p {:class "footer-line"}
        "Your final score reveals what kind of hand you have been. The seven principles will not be revealed until the end. Trust your instincts; we are watching them.")))

(defn- choices-list [scenario]
  (let [choices (:choices scenario)]
    (h :ol {:class "choices"}
       (map-indexed
        (fn [idx choice]
          (h :li nil
             (h :button {:class    "choice-btn"
                         :on-click (fn [_] (choose! choice))
                         :data-key (str idx)}
                (:label choice))))
        choices))))

(defn- view-playing [{:keys [index]}]
  (let [scenario (nth scenarios index)
        total    (count scenarios)]
    (h :div {:class "screen screen-playing"}
       (h :p {:class "eyebrow"}
          (str "scenario " (inc index) " of " total " · principle " (:principle scenario)))
       (h :h2 {:class "scenario-title"} (:title scenario))
       (h :div {:class "scenario-setup"
                :html  (inline-md (:setup scenario))})
       (choices-list scenario))))

(defn- view-resolving [{:keys [index current-choice]}]
  (let [scenario (nth scenarios index)
        kind     (:score current-choice)
        verdict  (case kind
                   :hygienic "hygienic"
                   :neutral  "neutral"
                   :heedless "heedless"
                   "neutral")
        last?    (= (inc index) (count scenarios))]
    (h :div {:class "screen screen-resolving"}
       (h :p {:class "eyebrow"}
          (str "principle " (:principle scenario) " · the consequence"))
       (h :p {:class (str "verdict verdict-" verdict)} verdict)
       (h :blockquote {:class "your-choice"} (:label current-choice))
       (h :div {:class "scenario-result"
                :html  (inline-md (:result current-choice))})
       (h :button {:class "primary cta-large" :on-click advance!}
          (if last? "See the diagnosis" "Continue")))))

(defn- view-finished [{:keys [chosen]}]
  (let [score    (scoring/tally chosen)
        {:keys [title body]} (scoring/diagnose score)
        per-p    (scoring/per-principle-summary scenarios chosen)]
    (h :div {:class "screen screen-finished"}
       (h :p {:class "eyebrow"} (str "final score · " (if (pos? score) "+") score " of 7"))
       (h :h1 {:class "diagnosis-title"} title)
       (h :p {:class "diagnosis-body"} body)
       (h :section {:class "summary"}
          (h :p {:class "summary-eyebrow"} "Your seven choices")
          (h :ul {:class "summary-list"}
             (map (fn [{:keys [id title principle score-symbol score-kind]}]
                    (h :li {:class (str "summary-row summary-" (name (or score-kind :neutral)))}
                       (h :span {:class "summary-sym"}  score-symbol)
                       (h :span {:class "summary-numeral"} principle)
                       (h :span {:class "summary-title"} title)))
                  per-p)))
       (h :div {:class "end-actions"}
          (h :button {:class "ghost" :on-click restart!} "Play again")
          (h :a {:class "ghost" :href "https://uprootiny.github.io/heedless-manifesto/"} "Read the manifesto"))
       (h :p {:class "footer-line"}
          "The discipline lives in the moments when speed feels rewarded. Practice the moment, not the principles."))))

;; -----------------------------------------------------------------------------
;; State transitions.
;; -----------------------------------------------------------------------------

(defn start-game! [_]
  (reset! state (assoc initial-state :phase :playing)))

(defn choose! [choice]
  (swap! state assoc
         :phase :resolving
         :current-choice choice
         :chosen (conj (:chosen @state) choice)))

(defn advance! [_]
  (let [{:keys [index]} @state
        next-index      (inc index)
        finished?       (>= next-index (count scenarios))]
    (if finished?
      (swap! state assoc :phase :finished :current-choice nil)
      (swap! state assoc :phase :playing :index next-index :current-choice nil))
    (scroll-top!)))

(defn restart! [_]
  (reset! state initial-state)
  (scroll-top!))

(defn scroll-top! []
  (.scrollTo js/window 0 0))

;; -----------------------------------------------------------------------------
;; Render loop — re-render on every state change.
;; -----------------------------------------------------------------------------

(defn render! []
  (let [root (gdom/getElement "app")
        s    @state
        view (case (:phase s)
               :intro     (view-intro)
               :playing   (view-playing s)
               :resolving (view-resolving s)
               :finished  (view-finished s))]
    (set! (.-innerHTML root) "")
    (.appendChild root view)))

(add-watch state :renderer
           (fn [_ _ _ _] (render!)))

(defn ^:export init []
  (render!))

(init)
