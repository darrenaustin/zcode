;(clojure/in-ns 'zstory)
;(clojure/refer 'clojure)
;(lib/use binary)

;; Story support

(def create-zui)

(defn create-story [source name data frames ui]
  {:source source
   :name name
   :mem (ref data)
   :frames (ref frames)
   :ui (ref ui)
   :breakpoints (ref #{})})

(defn create-frame [pc]
  {:pc (ref pc)
   :locals (ref nil)
   :stack (ref nil)
   :result-store (ref nil)})

(defn story-name [file-name]
  (let [file (. (java.io.File. file-name) (getName))
		story (re-find #"(.*)\\.(z[1-9]|dat)" file)]
	(if story
	  (story 1)
	  file)))

(defn load-story [file-name]
  (let [name (story-name file-name)
		data (bytes-from-file file-name)]
	(create-story file-name name data nil (create-zui name))))

(def current-story)

(defmacro with-story [story & body]
  `(binding [current-story ~story]
	 ~@body))

