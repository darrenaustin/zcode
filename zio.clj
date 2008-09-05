;(clojure/in-ns 'zio)
;(clojure/refer 'clojure)

(import '(javax.swing JFrame JScrollPane JTextPane))
(import '(java.awt Dimension Insets))
(import '(java.awt.event KeyEvent))

(defn create-text-pane []
  (doto (proxy [JTextPane] []
;		  (paint [g] (println "painting = " g))
;		  (processKeyEvent [#^KeyEvent e] (println "Key = " e) (error "Not printing key = " e)))
		  )
	(setMargin (Insets. 10 10 10 10))
	(setEditable true)))

(defn create-zui-frame [name text-pane]
  (doto (JFrame. name) 
	(add (doto (JScrollPane. text-pane)
		   (setVerticalScrollBarPolicy (JScrollPane/VERTICAL_SCROLLBAR_ALWAYS))
		   (setPreferredSize (Dimension. 600 600))
		   (setMinimumSize (Dimension. 10 10))))
	(pack)
	(show)))

(defn create-zui [name]
  (let [text (create-text-pane)
		doc (.getStyledDocument text)
		frame (create-zui-frame name text)
		append (fn [str] (.insertString doc (.getLength doc) str nil))]
	{:frame frame :doc doc :append append }))

(defn zprint [value] 
  ((:append @(:ui current-story)) (with-out-str (print value))))

(defn zprintln
  ([] (zprintln ""))
  ([value] ((:append @(:ui current-story)) (with-out-str (println value)))))
