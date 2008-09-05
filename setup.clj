(load-file "zcode.clj")

;(lib/use binary util)

;(clojure/in-ns 'zstory)

(def etude (load-story "games/etude.z5"))
(def enchanter (load-story "games/Enchanter.z3"))
(def jigsaw (load-story "games/Jigsaw.z8"))

(def current-story enchanter)
(push-initial-frame)

(comment
  (def current-story (load-story "games/gntests.z5"))
  (def current-story (load-story "games/Enchanter.z3"))
  (def current-story (load-story "games/Sorcerer.z3"))
  (def current-story (load-story "games/Jigsaw.z8"))
  (def current-story (load-story "games/A Mind Forever Voyaging.z4"))
  (def current-story (load-story "games/Zork Zero.z6"))
  (def current-story (load-story "games/milliways.z5"))
)

(defn ppc [] (hex (pc)))

(defn step []
  (let [breakpoints @(:breakpoints current-story)]
	(dosync
	 (ref-set (:breakpoints current-story) nil)
	 (execute-instruction (read-instruction))
	 (ref-set (:breakpoints current-story) breakpoints)
	 nil)))

(defn play-story [story]
  (with-story story
	(push-initial-frame)
	(loop []
	  (execute-instruction (read-instruction))
	  (recur))))

(defn execute []
  (when-not (at-breakpoint? (pc))
	(dosync 
	 (execute-instruction (read-instruction)))
	(recur)))

(defn peek-instruction []
  (let [current-pc (pc)
		ins (read-instruction)]
	(set-pc current-pc)
	ins))
