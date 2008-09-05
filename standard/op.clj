(defn op-data [filename]
  (map (fn [line] (apply vector (map (fn [token] (.trim token)) (seq (.split line "\\|")))))
	   (re-seq #".*\n" (slurp filename))))

(defn empty? [field]
  (zero? (.length field)))

(defn op-flags [op]
  (disj (-> #{} 
			(conj (if (empty? (nth op 0)) nil :store))
			(conj (if (empty? (nth op 1)) nil :branch)))
		nil))

(defn op-type [op]
  (symbol (str ":" (nth op 2))))

(defn op-code [op]
  (.parseInt Integer (nth op 3) 16))

(defn op-name [op]
  (symbol (nth op 6)))

(defn op-versions [op]
  (let [versions (nth op 4)]
	(if (empty? versions)
	  nil
	  (let [nums (apply vector (map #(.parseInt Integer %1) (seq (.split versions " "))))]
		(if (= (count nums) 1)
		  [(first nums)]
		  (apply vector (range (first nums) (inc (second nums)))))))))

(defn op-desc [op]
  (.replaceAll (nth op 5) " +" " "))

(defn convert-op [op]
  (let [name (op-name op)
		type (op-type op)
		code (op-code op)
		versions (op-versions op)
		flags (op-flags op)
		desc (op-desc op)]
	(list 'defop name type code versions flags desc)))

(defn convert [filename]
  (let [ops (map convert-op (op-data filename))]
	(with-open out (java.io.FileWriter. (str filename ".clj"))
	  (binding [*out* out]
		(doseq op ops 
		  (prn op)
		  (prn))))))
