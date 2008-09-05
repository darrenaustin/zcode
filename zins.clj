;(clojure/in-ns 'zins)
;(clojure/refer 'clojure)
;(clojure/refer 'util)
;(clojure/refer 'binary)
;(clojure/refer 'zstory)
;(clojure/refer 'zmem)
;(clojure/refer 'zstring)
;(clojure/refer 'zmachine)
;(clojure/refer 'zio)

;; Operations and instructions
(defstruct operation :name :type :opcode :versions :properties :desc :function)

(def empty-opcode-bucket (vec (replicate 9 nil)))

(def operations {:op0 (ref {})
				 :op1 (ref {})
				 :op2 (ref {})
				 :var (ref {})
				 :ext (ref {})})

(defn add-op-to-bucket [op versions bucket]
  (if versions
	(add-op-to-bucket op (rest versions)
					  (assoc bucket (first versions) op))
	bucket))

(defn add-operation [op]
  (let [opcode (:opcode op)
		versions (:versions op)
		type-ops ((:type op) operations)
		bucket (or (@type-ops opcode) empty-opcode-bucket)]
	(if (nil? versions)
	  (dosync (alter type-ops assoc opcode op))
	  (dosync (alter type-ops assoc opcode 
					 (add-op-to-bucket op versions bucket))))))

(defmulti get-operand-value first)
(defmethod get-operand-value nil [operand] nil)
(defmethod-for-values get-operand-value '(:large-constant :small-constant) [operand]
  (second operand))
(defmethod get-operand-value :variable [operand]
  (get-variable (second operand)))

(defmacro defop [name type opcode versions properties desc & body]
	 `(let [fn# ~(if body 
				  `(fn ~@body) 
				  `(fn [& rest#] (error '~name " not implemented yet")))
			op# (struct operation '~name ~type ~opcode ~versions ~properties ~desc fn#)]
		(add-operation op#)))

(load-file "zops.clj")

;; Instructions
(defstruct instruction :operation :operands :store :branch :text :pc)

(def opcode-form-variable 0xC0)
(def opcode-form-short 0x80)
(def opcode-form-extended 0xBE)

(defn opcode-form-basic [opcode-byte]
  (cond 
   (bits-set? opcode-form-variable opcode-byte) :variable
   (bits-set? opcode-form-short opcode-byte) :short
   :else :long))

(defmulti opcode-form version)
(defmethod-for-values opcode-form '(1 2 3 4) [opcode-byte] 
  (opcode-form-basic opcode-byte))
(defmethod opcode-form :default [opcode-byte]
  (if (= opcode-byte opcode-form-extended)
	:extended
	(opcode-form-basic opcode-byte)))

(defn read-opcode [opcode-byte form]
  (cond (= form :extended) (next-byte)
		(= form :short) (bit-and 0xF opcode-byte)
		(= form :long) (bit-and 0x1F opcode-byte)
		(= form :variable) (bit-and 0x1F opcode-byte)
		:else (error "Unknown opcode form: " form)))

(def operand-types 
  { 0 :large-constant, 1 :small-constant, 2 :variable, 3 :omitted })

(defn short-form-operand-type [opcode]
  (operand-types (bit-shift-right (bit-and 0x30 opcode) 4)))
	 
(defn long-form-operand-type [type]
  (if type :variable :small-constant))

(defn read-basic-variable-types []
  (let [type-byte (next-byte)]
	(loop [types-left type-byte
		   types []]
	  (let [type (operand-types (bit-shift-right (bit-and 0xC0 types-left) 6))]
		(if (or (= type :omitted) (= (count types) 4))
		  types
		  (recur (bit-shift-left types-left 2) (conj types type)))))))

(defn read-variable-types [opcode-byte]
  (if (or (= opcode-byte 236)
		  (= opcode-byte 250))
    ; special case the double variable call_vs2 and call_vn2 
    ; TODO: do we need constants for these?
	(vec (concat (read-basic-variable-types) 
				 (read-basic-variable-types)))
	(read-basic-variable-types)))

(defn read-operand [type]
 (cond 
  (= type :large-constant) (next-word)
  (= type :small-constant) (next-byte)
  (= type :variable) (next-byte)
  :else (error "Trying to read an omitted operand.")))

(defn read-operands-for-types [types]
  (loop [operands []
		 types-left types]
	(if (nil? types-left)
	  operands
	  (recur (conj operands [(first types-left) (read-operand (first types-left))])
			 (rest types-left)))))

(defmulti read-operands (fn [opcode-byte form] form))
(defmethod read-operands :long [opcode-byte form]
  (let [first-type (bit-test opcode-byte 6)
		second-type (bit-test opcode-byte 5)
		types [(long-form-operand-type first-type)
			   (long-form-operand-type second-type)]]
	(read-operands-for-types types)))

(defmethod read-operands :short [opcode-byte form]
	(let [type (short-form-operand-type opcode-byte)]
	  (if (not (= type :omitted))
		(read-operands-for-types [type]))))

(defmethod-for-values read-operands '(:variable :extended) [opcode-byte form]
  (let [types (read-variable-types opcode-byte)]
	(read-operands-for-types types)))

(def instruction-types { 0 :op0, 1 :op1, 2 :op2, :variable :var, :extended :ext })

(defn opcode-type [opcode-byte form operands]
  (if (= form :variable)
	(if (bit-test opcode-byte 5) :var :op2)
	(or (instruction-types form)
		(instruction-types (count operands)))))

(defn find-operation [type opcode]
  (let [bucket (@(operations type) opcode)]
	(cond 
	 (nil? bucket) (error "Unknown instruction: " type ":" opcode)
	 (vector? bucket) 
	   (let [op (nth bucket (version))]
		 (or op
			 (error "Illegal instruction for version " (version) ": " type ":" opcode)))
	 :else bucket)))

(defn read-store [operation]
  (if (:store (:properties operation))
	(next-byte)))

(defn read-branch [operation]
  (if (:branch (:properties operation))
	(let [branch-byte (next-byte)
		  branch-true (bit-test branch-byte 7)
		  branch-offset (if (bit-test branch-byte 6)
						  (bit-and branch-byte 0x3f)
						  (bit-or (bit-shift-left (bit-and branch-byte 0x3f) 8) (next-byte)))]
	  (cond (= branch-offset 0) [branch-true :ret-false]
			(= branch-offset 1) [branch-true :ret-true]
			:else [branch-true (+ (pc) branch-offset -2)]))))

(defn read-instruction-text [operation]
  (if (:text (:properties operation))
	(read-string-from (pc-ref))))

(defn read-instruction []
  (let [start-pc (pc)
		opcode-byte (next-byte)
		form (opcode-form opcode-byte)
		opcode (read-opcode opcode-byte form)
		operands (read-operands opcode-byte form)
		type (opcode-type opcode-byte form operands)
		operation (find-operation type opcode)
		store (read-store operation)
		branch (read-branch operation)
		text (read-instruction-text operation)]
	(struct instruction operation operands store branch text start-pc)))

(defn print-variable [var]
  (cond (= var 0x0) "(sp)"
		(<= 0x1 var 0xf) (format "L%x" var)
		:else (format "G%x" (- var 0x10))))

(defmulti print-operand first)
(defmethod print-operand nil [operand] "omitted")
(defmethod-for-values print-operand [:large-constant :small-constant] [operand]
  (format "#%x" (second operand)))
(defmethod print-operand :variable [operand]
  (print-variable (second operand)))

(defn print-operands [operands]
  (if operands
	(str (print-operand (first operands)) 
		 (if (rest operands) 
		   (str ", " (print-operands (rest operands))) ""))
	""))

(defn print-store [store]
  (str ">" (print-variable store)))

(defn print-branch [branch]
  (let [branch-bool (if (first branch) "T" "F")
		address (second branch)
		address? (not (keyword? address))
		value (cond address? address
					(= address :ret-true) "rtrue"
					:else "rfalse")]
  (format (if address? "^%s-%x" "^%s-%s") branch-bool value)))

(defn print-instruction [ins]
  (let [operands (:operands ins)
		store (:store ins)
		branch (:branch ins)
		text (:text ins)]
  (format "%x: %s %s %s %s %s"
		  (:pc ins)
		  (:name (:operation ins))
		  (if operands (print-operands operands) "")
		  (if store (print-store store) "")
		  (if branch (print-branch branch) "")
		  (if text (str "\"" text "\"") ""))))

(defn execute-instruction [ins]
  (let [op (:operation ins)
		func (:function op)]
	(println (print-instruction ins))
	(func (:operands ins) (:store ins) (:branch ins) (:text ins))))

