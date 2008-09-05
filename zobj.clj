;(clojure/in-ns 'zobj)
;(clojure/refer 'clojure)

(defn get-property-default [prop]
  (zword (+ (zword header-object-table)
			(* 2 (dec prop)))))

(defmulti object-table version)
(defmethod-for-values object-table [1 2 3] [] 
  (+ (zword header-object-table) 62)) ; 31 words
(defmethod object-table :default []
  (+ (zword header-object-table) 126)) ; 63 words

(defmulti object-address version)
(defmethod-for-values object-address [1 2 3] [obj]
  (+ (object-table) (* 9 (dec obj))))
(defmethod object-address :default [obj]
  (+ (object-table) (* 14 (dec obj))))

(defn max-attributes []
  (if (< (version) 4) 32 48))

(defn object-attribute? [obj attr]
  (let [obj-addr (object-address obj)
		attr-byte (zbyte (+ obj-addr (quot attr 8)))
		bit (- 7 (rem attr 8))]
	(bit-test attr-byte bit)))
	
(defn set-object-attribute 
  ([obj attr] (set-object-attribute obj attr true))
  ([obj attr set?]
	 (let [obj-addr (object-address obj)
		   attr-byte-addr (+ obj-addr (quot attr 8))
		   attr-byte (zbyte attr-byte-addr)
		   bit (- 7 (rem attr 8))
		   new-byte ((if set? bit-set bit-clear) attr-byte bit)]
	(set-zbyte attr-byte-addr new-byte))))

(defn clear-object-attribute [obj attr] 
  (set-object-attribute obj attr false))

(defn object-attributes [obj]
  (filter identity (map #(if (object-attribute? obj %1) %1) 
						(range 0 (max-attributes)))))

(defn object-parent [obj]
  (if (< (version) 4)
	(zbyte (+ (object-address obj) 4))
	(zword (+ (object-address obj) 6))))

(defn set-object-parent [obj parent]
  (if (< (version) 4)
	(set-zbyte (+ (object-address obj) 4) parent)
	(set-zword (+ (object-address obj) 6) parent)))

(defn object-sibling [obj]
  (if (< (version) 4)
	(zbyte (+ (object-address obj) 5))
	(zword (+ (object-address obj) 8))))

(defn set-object-sibling [obj sibling]
  (if (< (version) 4)
	(set-zbyte (+ (object-address obj) 5) sibling)
	(set-zword (+ (object-address obj) 8) sibling)))

(defn object-child [obj]
  (if (< (version) 4)
	(zbyte (+ (object-address obj) 6))
	(zword (+ (object-address obj) 10))))

(defn set-object-child [obj child]
  (if (< (version) 4)
	(set-zbyte (+ (object-address obj) 6) child)
	(zword (+ (object-address obj) 10) child)))

(defn object-remove [obj]
  (let [parent (object-parent obj)
		sibling (object-sibling obj)
		first-child (object-child parent)]
	(set-object-parent obj 0)
	(set-object-sibling obj 0)
	(if (not (zero? parent))
	  (if (= first-child obj)
		; delete from the front of the children
		(set-object-child parent sibling)

        ; remove from the middle of the children
		(loop [prev first-child]
		  (let [prev-sib (object-sibling prev)]
			(if (= prev-sib obj)
			  ; found previous
			  (set-object-sibling prev sibling)
			  ; keep looking
			  (recur prev-sib))))))))

(defn object-insert [obj parent]
  (object-remove obj)
  (set-object-sibling obj (object-child parent))
  (set-object-parent obj parent)
  (set-object-child parent obj))

(defn object-property-table [obj]
  (zword (+ (object-address obj) 
			(if (< (version) 4) 7 12))))

(defn object-description [obj]
  (let [props (object-property-table obj)
		length (zbyte props)
		desc (read-string (inc props) length)]
	desc))

(defn bytes-at [addr num]
  (loop [bytes []
		 a addr
		 n num]
	(if (zero? n)
	  bytes
	  (recur (conj bytes (hex (zbyte a))) (inc a) (dec n)))))

(defn object-first-prop-addr [obj]
  (let [table (object-property-table obj)]
	(+ table 1 (* (zbyte table) 2))))

(defmulti object-get-prop-info version)
; return a vector of [prop-num size data-addr] or nil if no property at addr
(defmethod-for-values object-get-prop-info [1 2 3] [addr]
  (let [size-byte (zbyte addr)
		prop-num (rem size-byte 32)
		size (inc (quot (- size-byte prop-num) 32))
		data-addr (inc addr)]
	(if (zero? size-byte)
	  nil
	  [prop-num size data-addr])))

(defmethod object-get-prop-info :default [addr]
  (let [size-byte (zbyte addr)]
	(if (zero? size-byte)
	  nil
	  (if (bit-test size-byte 7)
		(let [prop-num (bit-and 0x3f size-byte) ; 2-byte size
			  size (bit-and 0x3f (zbyte (inc addr)))
			  data-addr (+ addr 2)] 
		  [prop-num size data-addr])
		(let [prop-num (bit-and 0x3f size-byte) ; single byte case
			  size (if (bit-test size-byte 6) 2 1)
			  data-addr (inc addr)]
		  [prop-num size data-addr])))))

(defn object-prop-info [obj prop]
  (loop [addr (object-first-prop-addr obj)]
	(let [prop-info (object-get-prop-info addr)]
	  (if prop-info
		(let [[prop-num size data-addr] prop-info]
		  (if (= prop-num prop)
			prop-info
			(recur (+ data-addr size))))))))

(defn object-next-prop [obj prop]
  (if (zero? prop)
	; Get the first property
	(let [[prop-num size data-addr] 
		  (object-get-prop-info (object-first-prop-addr obj))]
	  prop-num)
	; Find the property and then the next one
	(let [[prop-num size data-addr] (object-prop-info obj prop)]
	  (if (nil? prop-num)
		(error "Can't get next property when property " prop 
			   " is not a property of object " obj)
		(let [[prop-num size data-addr] 
			  (object-get-prop-info (+ data-addr size))]
		  (if prop-num prop-num 0))))))

(defn object-property [obj prop]
  (let [[prop-num size data-addr] (object-prop-info obj prop)]
	(if prop-num
	  (cond (= size 1) (zbyte data-addr)
			(= size 2) (zword data-addr)
			:else (error "Property size too large for get_prop: " size))
	  (get-property-default prop))))

(defn object-property-addr [obj prop]
  (let [[prop-num size data-addr] (object-prop-info obj prop)]
	(if data-addr data-addr 0)))

(defmulti object-property-len version)
(defmethod-for-values object-property-len [1 2 3] [prop-addr]
  (let [size-byte (zbyte (dec prop-addr))
		prop-num (rem size-byte 32)
		size (inc (quot (- size-byte prop-num) 32))]
	size))
(defmethod object-property-len :default [prop-addr]
  (let [size-byte (zbyte (dec prop-addr))]
	(if (bit-test size-byte 7)
	  ; 2-byte size
	  (bit-and 0x3f size-byte)
	  ; 1-byte size
	  (if (bit-test size-byte 6) 2 1))))

(defn object-put-property [obj prop value]
  (let [[prop-num size data-addr] (object-prop-info obj prop)]
	(cond 
	 (nil? size) (error "Can't change property " prop 
						", as it doesn't exist on object " obj)
	 (= size 1) (set-zbyte data-addr (bit-and 0xff value))
	 (= size 2) (set-zword data-addr (bit-and 0xffff value))
	 :else (error "Can't set a prop of size greater than 2 bytes"))))

(defn object-properties [obj]
  (loop [addr (object-first-prop-addr obj)
		 props []]
	(let [prop-info (object-get-prop-info addr)]
	  (if (nil? prop-info)
		props
		(let [[prop-num size data-addr] prop-info]
		  (recur (+ data-addr size)
				 (conj props [prop-num (bytes-at data-addr size)])))))))

(def dump-object-tree)

(defn dump-siblings [obj prefix]
  (let [sibling-num (object-sibling obj)
		siblings (if (not (zero? sibling-num))
				  (dump-siblings sibling-num prefix))]
	(str (dump-object-tree obj prefix) siblings)))

(defn dump-object-tree 
  ([obj] (dump-object-tree obj ""))
  ([obj prefix]
	 (let [child-num (object-child obj)
		   children (if (not (zero? child-num))
					  (dump-siblings child-num (str prefix "  "))
					  "")]
	   (str prefix obj " " (object-description obj) "\n" children))))
