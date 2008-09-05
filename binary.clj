;(clojure/in-ns 'binary)
;(clojure/refer 'clojure)

;(lib/use 'util)

(defn bytes-from-file [file-name]
  (let [rdr (java.io.FileInputStream. file-name)]
	(loop [bytes []]
	  (let [b (.read rdr)]
		(if (not (= b -1))
		  (recur (conj bytes b))
		  bytes)))))

(defn byte-at [data address]
  (data address))

(defn set-byte-at [data address value]
  (assoc data address value))

(defn word-at [data address]
  (+ (bit-shift-left (data address) 8) 
	 (data (inc address))))

(defn set-word-at [data address value]
  (let [high (bit-shift-right (bit-and 0xFF00 value) 8)
		low  (bit-and 0x00FF value)]
	(assoc data address high (inc address) low)))

(defn bits-set? [bits value]
  (= (bit-and bits value) bits))

(defn signed-word [word]
  (if (bit-test word 15)
	(- word 0x10000)
	word))

