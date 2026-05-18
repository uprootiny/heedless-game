# heedless-game

A choose-your-own-adventure decision tree built on the [heedless-manifesto](https://github.com/uprootiny/heedless-manifesto). Play as an agent in a shared workshop and find out what kind of hand you are.

Live: **[uprootiny.github.io/heedless-game](https://uprootiny.github.io/heedless-game/)**

## What it is

Seven scenarios, one per principle of the manifesto. Each scenario presents a real-feeling situation an AI agent might face, with three or four choices. Each choice is scored on a hygiene rubric (hygienic, neutral, heedless). At the end, your seven choices add up to a diagnosis.

The principles are not revealed until the end. The point is to play the moment, not the principle.

## Stack

- **ClojureScript** + plain `goog.dom` (no React, no Reagent). The game is small enough that hand-rolled DOM is the proportionate choice.
- Compiled with the official ClojureScript compiler via the Clojure CLI. No Node, no npm.
- ~138 KB compiled JS in `:advanced` optimization mode — the bulk is the Closure / cljs core libs, the game itself is a few KB.

## Build

```sh
clojure -M:build      # advanced-optimized compile to docs/main.js
clojure -M:dev        # unoptimized dev build with source maps
```

The compiled `docs/main.js` is committed so GitHub Pages can serve the site without a build step.

## Sibling projects

- **[heedless-manifesto](https://github.com/uprootiny/heedless-manifesto)** — the principles, in polyphonic form.
- **[heedless](https://github.com/uprootiny/heedless)** — landing page.

## License

The principles are not owned. The scenarios are offered as illustration. Pull requests are welcome.
