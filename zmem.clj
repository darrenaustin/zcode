;(clojure/in-ns 'zmem)
;(clojure/refer 'clojure)
;(clojure/refer 'util)
;(clojure/refer 'binary)
;(clojure/refer 'zstory)

;; Memory
(defn zbyte [byte-addr]
  (byte-at @(current-story :mem) byte-addr))

(defn set-zbyte [byte-addr value]
  (let [mem-ref (:mem current-story)]
	(dosync 
	 (ref-set mem-ref (set-byte-at @mem-ref byte-addr value)))))

(defn zword [word-addr]
  (word-at @(current-story :mem) word-addr))

(defn set-zword [word-addr value]
  (let [mem-ref (:mem current-story)]
	(dosync 
	 (ref-set mem-ref (set-word-at @mem-ref word-addr value)))))

(defn version 
  ([] (version nil))
  ([&rest] (zbyte 0)))

;; Header addresses
(def header-main-address 0x06)
(def header-dictionary-table 0x08)
(def header-object-table 0x0a)
(def header-global-variable-table 0xc)
(def header-abbreviations-table 0x18)
(def header-routine-offset 0x28)
(def header-string-offset 0x2a)
(def header-alphabet-address 0x34)

;; Addressing
(defmulti routine-address version)
(defmethod-for-values routine-address '(1 2 3) [packed] (* 2 packed))
(defmethod-for-values routine-address '(4 5) [packed] (* 4 packed))
(defmethod-for-values routine-address '(6 7) [packed] 
  (+ (* 4 packed) (* 8 (zword header-routine-offset))))
(defmethod-for-values routine-address '(8) [packed] (* 8 packed))

(defmulti string-address version)
(defmethod-for-values string-address '(1 2 3) [packed] (* 2 packed))
(defmethod-for-values string-address '(4 5) [packed] (* 4 packed))
(defmethod-for-values string-address '(6 7) [packed] 
  (+ (* 4 packed) (* 8 (zword header-string-offset))))
(defmethod-for-values string-address '(8) [packed] (* 8 packed))
