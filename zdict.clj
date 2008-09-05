;(clojure/in-ns 'zdict)
;(clojure/refer 'clojure)

(defn word-separators [] 
  (let [table (zword header-dictionary-table)
		num (zbyte table)]
	(loop [separators nil
		   num-left num]
	  (if (zero? num-left)
		separators
		(recur (conj separators (char (zbyte (+ table num-left))))
			   (dec num-left))))))

(defn dict-entry-size []
  (let [table (zword header-dictionary-table)
		num-separators (zbyte table)]
	(zbyte (+ table 1 num-separators))))

(defn dict-num-enties []
  (let [table (zword header-dictionary-table)
		num-separators (zbyte table)]
	(zword (+ table 2 num-separators))))

(defn first-dict-entry-addr []
  (let [table (zword header-dictionary-table)
		num-separators (zbyte table)]
	(+ table num-separators 4)))

(defn dict-entry [num]
  (let [table (first-dict-entry-addr)
		word (read-string (+ table (* (dec num) (dict-entry-size))))]
	word))

		