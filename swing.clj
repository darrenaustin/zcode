(import '(javax.swing JFrame JTextPane))
(import '(java.awt.event KeyListener KeyEvent))

(defstruct editor :frame :text-pane :doc)

(defn create-editor [name]
  (let [frame (JFrame. name)
		text (JTextPane.)]
	(doto frame (add text) (pack) (show))
	(struct editor frame text (.getStyledDocument text))))

(defn append [editor s]
  (let [d (:doc editor)
		end (.getLength d)]
	(.insertString d end s nil)))

(defn setEditable [editor edit]
  (.setEditable (:text-pane editor) edit))

(defn addListener [editor]
  (.addKeyListener (:text-pane editor)
    (proxy [KeyListener] []
      (keyPressed [#^KeyEvent e] )
	  (keyTyped [#^KeyEvent e] (append editor (str (.getKeyChar e))))
	  (keyReleased [#^KeyEvent e] ))))
