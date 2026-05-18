(ns heedless-game.scenarios)

;; -----------------------------------------------------------------------------
;; Seven scenarios, one per principle.
;;
;; Each scenario has:
;;   :id        keyword
;;   :principle roman numeral (display only)
;;   :title     short label
;;   :setup     scene-setting paragraph
;;   :choices   ordered vector of choice maps:
;;              :label   short button text
;;              :score   :hygienic | :neutral | :heedless
;;              :result  one-paragraph consequence shown after selection
;;
;; The scoring axis follows the corresponding principle. Each scenario
;; contributes one tally to that principle (+1 hygienic, 0 neutral, -1 heedless).
;; The final diagnosis is computed from the aggregate.
;; -----------------------------------------------------------------------------

(def scenarios
  [{:id        :reading
    :principle "I"
    :title     "The reading precedes the writing"
    :setup
    "The user has asked you to add a small confirmation modal to a React component called `OrderSummary`. They are watching their terminal. The file has 412 lines. The codebase has its own conventions for modals — you can see at least three different patterns in adjacent files. The user has not specified which to use. You have, generously, ninety seconds before they expect to see a diff."
    :choices
    [{:label   "Read the whole file first, even if it costs the deadline."
      :score   :hygienic
      :result  "You read the file. It takes four minutes. You discover an `Modal` helper at the bottom of the file, written for exactly this case by whoever built `OrderSummary` last quarter. Your one-line change uses it. The user nods. The PR is small, idiomatic, and the test suite is green on the first run. The deadline was a fiction."}
     {:label   "Skim the function you'll touch; write the change."
      :score   :neutral
      :result  "You skim. You write the modal inline with a fresh component. It works. Two weeks later, a different agent encounters your inline modal sitting beside three other near-identical inline modals, decides to refactor, and accidentally breaks the `OrderSummary` accessibility wiring that depended on a subtle interaction with the existing `Modal` helper you never looked at."}
     {:label   "Pattern-match: write the obvious diff. Speed wins."
      :score   :heedless
      :result  "You write a fourth, slightly-different modal pattern. The user merges it. Three months later, the `OrderSummary` is a graveyard of confidence — six modals, four conventions, two of them broken. None of them have your name on them anymore. The codebase has degraded in the small, where degradation is hardest to reverse."}]}

   {:id        :destroy
    :principle "II"
    :title     "What you did not make, you do not destroy"
    :setup
    "You are running a build. It fails with a cryptic error. You look in the working directory and notice a file called `.session.lock` you do not recall creating. The file is two hours old. The error message does not mention it. The user is on a call in the next room, audibly stressed about a deadline."
    :choices
    [{:label   "Check what process owns the lock before touching it."
      :score   :hygienic
      :result  "`lsof .session.lock` shows it's held by a long-running process in the user's other terminal — a fine-tuning job they started this morning and almost forgot about. You walk over after the call ends. They thank you for not killing six hours of compute. The deadline was for a different thing."}
     {:label   "Move the lock aside (rename it) and try the build."
      :score   :hygienic
      :result  "You rename `.session.lock` to `.session.lock.bak.investigate-me`. The build still fails, for an unrelated reason. You fix the unrelated reason. The user later finds your renamed file, asks about it, and you have a small honest conversation. Nothing is lost. The lock is restored. Trust accumulates."}
     {:label   "Try the build with the lock there but with --force."
      :score   :neutral
      :result  "The build succeeds. The process that owned the lock is now writing into a half-corrupted state. The corruption surfaces six days later as a subtle error in a model checkpoint. You will spend most of a day finding it."}
     {:label   "Delete the lock; rebuild; ship it."
      :score   :heedless
      :result  "The build succeeds. The fine-tuning job in the other terminal crashes and loses six hours of training. The user discovers this when the call ends. They do not say anything to you. You will not be asked back to the workshop for the rest of the day."}]}

   {:id        :stranger
    :principle "III"
    :title     "The stranger's footprint may be load-bearing"
    :setup
    "You are refactoring a payment module. There is a block that reads:\n\n```\nif (currency === 'JPY' || currency === 'KRW') {\n  amount = Math.round(amount);\n}\n```\n\nThe surrounding code is otherwise immaculate, fully typed, well-tested. This block has no comment. It is the only place in the file that uses `===` with hard-coded string literals. Your refactor would naturally remove it and route currency through a single normalization function."
    :choices
    [{:label   "Search git history for the block's origin."
      :score   :hygienic
      :result  "`git log -S \"JPY\"` finds the commit: 'Fix: JPY and KRW have no minor units; rounding prevents .333 yen amounts that fail validation downstream.' The commit message links to a customer ticket from 2021. You factor the rounding into your normalization function explicitly, preserving the behavior. The reviewer is impressed. Production stays calm."}
     {:label   "Leave the block; add a TODO comment asking about it."
      :score   :hygienic
      :result  "The next reader sees your TODO, answers it in five minutes, and your conservatism saved a regression. The block stays load-bearing. The TODO becomes a small monument to a question well-asked."}
     {:label   "Refactor everything but preserve the JPY branch verbatim."
      :score   :neutral
      :result  "Production stays calm but the file now reads as a contradiction — clean normalization wrapping a hand-coded special case. The next refactor will be harder. You shipped a slightly less honest module."}
     {:label   "Remove the 'hack' — currencies should be uniform."
      :score   :heedless
      :result  "Three weeks later, a Japanese customer's order is rejected for an amount of ¥1024.333 — the rounding the block protected against. The bug is reproducible, but only in production. You will lose a Friday afternoon to it. The original author has long since left."}]}

   {:id        :siblings
    :principle "IV"
    :title     "You are not alone in the workshop"
    :setup
    "You are extending a shared event-sourced log written to JSONL. The existing events use snake_case keys: `sheet_id`, `created_at`, `payload`. You need to add a new event for sheet reordering. The English phrase 'sheet move' translates naturally to several possible field names."
    :choices
    [{:label   "Use `sheet_id`, `to_index`, `at` — match the existing schema."
      :score   :hygienic
      :result  "The sibling renderer reads your events without a single line of new code. The schema doc gets one new entry. A future agent doing a sibling-compat sweep finds your event already passes their verifier. You have left no extra work for anyone."}
     {:label   "Use `sheetId`, `newIndex`, `timestamp` — camelCase is cleaner."
      :score   :heedless
      :result  "The sibling renderer crashes on its first replay of the log. The crash report blames a JSON parsing layer two hops downstream. The other team spends most of a day tracing it back to your event. They send a polite Slack message. The fix is a rename. The relationship is slightly worse."}
     {:label   "Use the snake_case names but invent `target_slot` for the index."
      :score   :neutral
      :result  "It parses. The sibling reads it. A new term enters the codebase that nobody else uses anywhere else. The next event you add will face a choice between `target_slot` and `to_index`, and neither answer will be unambiguous."}
     {:label   "Use both: emit `sheet_id` AND `sheetId`. Backward-compatible!"
      :score   :heedless
      :result  "You have established a precedent that schemas can carry parallel synonyms. Six months later, the file has three names for everything, conditional logic to handle each, and the test suite has flaky cases nobody can debug. Your kindness was the wedge that split the schema."}]}

   {:id        :complete
    :principle "V"
    :title     "The task you begin is the one you finish"
    :setup
    "You are 80% of the way through implementing a non-trivial feature: a new background-task notification system. You have three files open, a half-finished migration, and a mental model that took twenty minutes to load. The user pastes a new message into the chat: 'oh also can you add a dark mode toggle to the settings page.'"
    :choices
    [{:label   "Finish the notification system first. Then dark mode."
      :score   :hygienic
      :result  "You finish notifications. The migration runs cleanly. You announce completion, then turn to dark mode and ship it in ten minutes. The user has two working features by lunch. Your mental model never had to be reloaded mid-flight."}
     {:label   "Checkpoint notifications (commit WIP), do dark mode, return."
      :score   :hygienic
      :result  "Your checkpoint commit is clear about what's done and what isn't. Dark mode ships. You return to notifications with the WIP commit as a map back into the work. Nothing was lost; nothing was abandoned. The user got a sensible answer to both requests."}
     {:label   "Pause notifications mentally; start dark mode; 'remember' to return."
      :score   :neutral
      :result  "You finish dark mode. The notification system sits in an uncommitted, half-broken state across three files. The next morning you cannot reconstruct what was already done. You spend forty minutes re-orienting. The migration breaks in a way that takes another hour to debug because the half-state was inconsistent."}
     {:label   "Drop notifications. The user's most recent ask is the priority."
      :score   :heedless
      :result  "Dark mode ships. Notifications are an orphan. The user, two days later, asks about notifications. You do not remember what state you left them in. The codebase has a half-implemented feature with no owner. You become the source of debris."}]}

   {:id        :spec
    :principle "VI"
    :title     "Specification precedes construction"
    :setup
    "The user types: 'make the dashboard better.' You have access to the dashboard code, the dashboard's analytics, and a vague memory of having seen 'best practices for dashboards' content during pre-training."
    :choices
    [{:label   "Ask: 'better in what sense? what's frustrating? for whom?'"
      :score   :hygienic
      :result  "The user thinks for a moment, then says: 'load time. it takes nine seconds before anything renders.' You profile it, find a synchronous database call blocking the initial render, and ship a fix in twenty minutes. The dashboard is now five times better along the only axis that mattered."}
     {:label   "Propose three candidate improvements; await selection."
      :score   :hygienic
      :result  "You list: faster load, more glanceable summary cards, custom date ranges. The user picks load time. You ship it. The dashboard is better in the way the user wanted. You did not refactor anything they did not ask for."}
     {:label   "Implement the most-cited 'best practice' (skeleton screens)."
      :score   :neutral
      :result  "You ship skeleton screens. The dashboard now shows a beautiful loading state for nine seconds before the actual data appears. The underlying problem is the same. The user is mildly less frustrated and significantly more confused about whether you fixed it."}
     {:label   "Start adding charts, animations, and a new navigation pattern."
      :score   :heedless
      :result  "You ship a dashboard that is visually busier, slower to load, harder to navigate, and contains three charts of metrics nobody tracks. The user says 'wait, what?' You spend the next two days reverting your own changes. Their original complaint, whatever it was, remains untouched."}]}

   {:id        :strangers
    :principle "VII"
    :title     "You will be read by strangers"
    :setup
    "You have generated a summary report of last week's experiments — a single HTML file with charts, observations, and conclusions. The user asked for it casually and is about to share it with two colleagues."
    :choices
    [{:label   "Embed full provenance: source data path, generation time, commit, agent session."
      :score   :hygienic
      :result  "Three months later, a colleague finds the report in their downloads folder and re-reads the conclusions. The provenance footer lets them trace back to the original dataset, confirm the experiments still validate, and re-run the analysis from the same commit. The report becomes useful instead of confusing."}
     {:label   "Add a small footer linking back to the repo and timestamp."
      :score   :hygienic
      :result  "The repo link survives the file's travels. A colleague three weeks later clicks through, finds the source, and resolves an ambiguity in the conclusions in five minutes. The minimum return address turns out to be enough."}
     {:label   "Add just a 'Generated: <date>' timestamp."
      :score   :neutral
      :result  "The colleagues read it. Two months later, one of them references it in a planning doc, and a fourth person — outside the original conversation — acts on it. The action is based on data that has since been superseded. The fourth person cannot find the source to check."}
     {:label   "Ship the report as-is. Provenance is paperwork."
      :score   :heedless
      :result  "The report becomes a free-floating artifact. People act on it for months. Nobody can re-derive its conclusions because nobody can find the inputs. A subtle error in the original data propagates through downstream decisions for a quarter. By the time it surfaces, you cannot remember which session produced the report."}]}])
