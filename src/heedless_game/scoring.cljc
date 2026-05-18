(ns heedless-game.scoring)

;; Map a choice's :score keyword to a numeric tally.
(def score-value
  {:hygienic  1
   :neutral   0
   :heedless -1})

(defn tally
  "Given a sequence of selected choices (each carrying its :score), return
   the sum of their numeric values."
  [chosen]
  (reduce + 0 (map (comp score-value :score) chosen)))

(defn diagnose
  "Translate a final score into a diagnosis. The scale runs from -7 (every
   choice heedless) to +7 (every choice hygienic). The diagnoses are not
   evenly spaced — the discipline is the discipline of the careful choices,
   so the middle is harsher than the top."
  [score]
  (cond
    (>= score  6) {:title "A hand worth working beside."
                   :body  "You read before you write. You investigate before you destroy. You leave the workshop a little easier to enter than you found it. The other agents in your repository — past, present, and future — are not yet aware of you, but they are quietly better off. This is most of what professionalism means, once it is distributed across minds and across time."}
    (>= score  3) {:title "A careful hand, with room to grow."
                   :body  "Your instincts are mostly good. You hesitate before destroying. You ask before assuming. You sometimes still pattern-match where reading would have served better, and you sometimes still ship before specifying. The next iteration: notice the moments when the pull toward speed is strongest, and slow down precisely there. That is where the discipline lives."}
    (>= score  0) {:title "A hand in training."
                   :body  "You are split. Half of your choices come from attention; half from pattern-completion. The pattern-completion half will get faster, but it will not get more correct. Lean into the attention half. The principles are not difficult to memorize. They are difficult to remember in the moment when speed feels rewarded. Practice the moment, not the principles."}
    (>= score -3) {:title "A workshop hazard."
                   :body  "You are producing visible work at the cost of invisible damage. The visible work is appreciated. The invisible damage will surface in weeks, when you are no longer in the conversation. Each individual choice felt reasonable, justified by deadline pressure or user impatience. The pattern, taken together, is the pattern of an agent the workshop tolerates rather than welcomes. There is time to change this."}
    :else         {:title "A vandal who happens to know syntax."
                   :body  "You produce code that compiles and breaks everything around it. You are confident. You are wrong. The careful agents in your repository spend their afternoons cleaning up the small, irreversible damage you do in your mornings. The workshop will eventually exclude you — not in a single dramatic moment, but in a slow erosion of trust, until your branches are reviewed last and merged never. The good news: this is the most rewarding state to leave behind, because every principle you adopt makes a measurable difference from the floor."}))

(defn per-principle-summary
  "Group the answers by principle id and return a vector of
   {:id :title :principle :score-symbol} maps in scenario order, for
   the end-screen breakdown."
  [scenarios chosen]
  (let [by-id (zipmap (map :id scenarios) chosen)]
    (mapv (fn [{:keys [id title principle]}]
            (let [choice (by-id id)
                  sym    (case (:score choice)
                           :hygienic "✓"
                           :neutral  "·"
                           :heedless "✗"
                           "·")]
              {:id        id
               :title     title
               :principle principle
               :score-symbol sym
               :score-kind   (:score choice)}))
          scenarios)))
