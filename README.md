# Using Storybook v8+ with CLJS

A basic showcase of how to use [storybook](https://storybook.js.org/) with shadow-cljs. Storybook in recent versions
changed how it discovers and handles stories with the [Component Story Format (CSF)](https://storybook.js.org/docs/api/csf). It no longer seems to do this at runtime, but rather by statically analyzing the JS code directly at build time.

It doesn't understand ClojureScript though, so the way you'd previously used storybook with CLJS no longer works.

I'm not a Storybook user myself, but I wanted to outline how you can get something usable without too much trouble. I picked [Reagent](https://github.com/reagent-project/reagent) for this showcase, but this could easily be adapted to any common CLJS library.

## Project Structure

First the basic CLJS project structure was created using the extremely simplistic `create-cljs-project` helper.

```
npx create-cljs-project foo
cd foo
```

Since I'd rather not mix any of the files created by `storybook` with the actual CLJS files I opted to create them in a dedicated `sb` folder in the project itself.

```
mkdir sb
cd sb
npm init -y
npm install react react-dom
npx storybook@latest init
```

This ends up creating a `stories` folder and a bunch of other stuff. I opted to use `vite` when asked by the installer script. I didn't change any of the supporting files and instead directly went to write the CLJS bits.

The last command already fires up the `storybook` process.

I then started shadow-cljs via `npx shadow-cljs server` and added the build config. I then used the UI at http://localhost:9630 to start the `:storybook` build watch. You could of course also just run `npx shadow-cljs watch storybook`.

I began by porting the `Button.js` file over to CLJS with this code:

```clojure
(ns example.components.button)

(defn button [{:keys [label primary size background-color on-click]}]
  (let [mode (if primary
               "storybook-button--primary"
               "storybook-button--secondary")]

    [:button
     {:type "button"
      :class ["storybook-button" (str "storybook-button--" size) mode]
      :on-click on-click
      :style (when background-color {:backgroundColor background-color})}
     label]))
```

Not a literal translation but close enough. The goal behind this was that this file has no trace of storybook whatsoever. Just regular `reagent` code you'd normally write.

### Keeping Stories in JS

I couldn't find a proper way to teach `storybook` about CLJS, and they seem really intent on you using their CSF format. Much of the tooling expects to work with JS files, and it seems like the easiest way to get access to most features was to just use JS to write the stories boilerplate code.

So, my goal was still keeping the `Button.stories.js` file, but replace the actual component code with the CLJS above. To do this I adjusted the `sb/stories/Button.stories.js` file accordingly.

First the code, then I'll highlight the important bits.

```
import PropTypes from 'prop-types';

import "./button.css";

import { wrap } from "./cljs/example.reagent-glue";
import "./cljs/example.components.button";
const Button = wrap("example.components.button", "button");

Button.propTypes = {
  /** Is this the principal call to action on the page? */
  primary: PropTypes.bool,
  /** What background color to use */
  backgroundColor: PropTypes.string,
  /** How large should the button be? */
  size: PropTypes.oneOf(['small', 'medium', 'large']),
  /** Button contents */
  label: PropTypes.string.isRequired,
  /** Optional click handler */
  onClick: PropTypes.func,
};

Button.defaultProps = {
  backgroundColor: null,
  primary: false,
  size: 'medium',
  onClick: undefined,
};

// remainder of the file left as-is
```

The only important bit was changing

```js
import { Button } from './Button';
```

to this

```js
import { wrap } from "./cljs/example.reagent-glue";
import "./cljs/example.components.button";
const Button = wrap("example.components.button", "button");
```

As previously said, the main intent is keeping everything storybook related out of the CLJS file. So, the necessary glue code lives here instead. First this imports the `wrap` function which is basically just a helper to make handling the JS<->CLJS interop a bit easier. Then it imports the component file and then wraps the actual component by name.

Now this has a `Button` local that pretty much acts exactly as the raw JS version did. I opted to move the `propTypes` stuff into the story file, since I don't think this is something you'd usually use in `reagent`. Seems to fit better thematically anyway.

After that I opened the story in the storybook web UI and everything seems to just work. I can adjust the `props` object from the UI and changes show up immediately. I can make changes to the CLJS code and storybook will take care of reloading everything, so it shows up seamlessly. I couldn't find a way to turn off the storybook reloading side of things, so I didn't bother trying to hook up the shadow-cljs hot-reload as it didn't seem necessary.

Again, I'm not an actual storybook user, but the mission seems accomplished at this point.

## Quick Note: `wrap`

This function is intentionally simple and doesn't do much. It might make sense to further adjust this, and it will not work in `release` builds as the `goog.getObjectByName` lookup will not work. Someone with actual intentions of using this might want to turn it into a reusable library. Feel free to steal any of this code.

```clj
(defn ^:export wrap [ns component-name]
  (fn [js-opts]
    (let [opts (to-clj js-opts {:key-fn camel->kw})
          cmp (js/goog.getObjectByName (str ns "." component-name))]
      ;; (js/console.log "using opts" js-opts opts)
      (r/as-element [cmp opts]))))
```

## Quick Note: `release`

`watch/compile` builds work fine, since they do not munge names, but `release` would. So if you need this to work with `release` code you'll need to make certain adjustments. Happy to explain in detail if anyone is interested.