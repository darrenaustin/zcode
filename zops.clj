(defn op-call [ops store]
  (let [address (routine-address (get-operand-value (ops 0)))]
	(call-routine address 
				  (vec (map get-operand-value (rest ops)))
				  store)))

(defop je :op2 1 nil #{:branch} "je a b ?(label)"
  [ops store branch text]
  (let [a (get-operand-value (ops 0))
		b (get-operand-value (ops 1))]
	(jump-if (= a b) branch)))

(defop jl :op2 2 nil #{:branch} "jl a b ?(label)"
  [ops store branch text]
  (let [a (get-operand-value (ops 0))
		b (get-operand-value (ops 1))]
	(jump-if (< a b) branch)))

(defop jg :op2 3 nil #{:branch} "jg a b ?(label)"
  [ops store branch text]
  (let [a (get-operand-value (ops 0))
		b (get-operand-value (ops 1))]
	(jump-if (> a b) branch)))

(defop dec_chk :op2 4 nil #{:branch} "dec_chk (variable) value ?(label)"
  [ops store branch text]
  (let [var (second (ops 0))
		value (get-operand-value (ops 1))]
	(dec-variable var)
	(jump-if (< (get-variable var) value) branch)))

(defop inc_chk :op2 5 nil #{:branch} "inc_chk (variable) value ?(label)"
  [ops store branch text]
  (let [var (second (ops 0))
		value (get-operand-value (ops 1))]
	(inc-variable var)
	(jump-if (> (get-variable var) value) branch)))

(defop jin :op2 6 nil #{:branch} "jin obj1 obj2 ?(label)"
  [ops store branch text]
  (let [obj1 (get-operand-value (ops 0))
		obj2 (get-operand-value (ops 1))]
	(jump-if (= (object-parent obj1) obj2) branch)))

(defop test :op2 7 nil #{:branch} "test bitmap flags ?(label)")

(defop or :op2 8 nil #{:store} "or a b -> (result)")

(defop and :op2 9 nil #{:store} "and a b -> (result)"
  [ops store branch text]
  (let [a (get-operand-value (ops 0))
		b (get-operand-value (ops 1))]
	(set-variable store (bit-and a b))))

(defop test_attr :op2 10 nil #{:branch} "test_attr object attribute ?(label)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		attr (get-operand-value (ops 1))]
	(jump-if (object-attribute? obj attr) branch)))

(defop set_attr :op2 11 nil #{} "set_attr object attribute"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		attr (get-operand-value (ops 1))]
	(set-object-attribute obj attr)))

(defop clear_attr :op2 12 nil #{} "clear_attr object attribute"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		attr (get-operand-value (ops 1))]
	(clear-object-attribute obj attr)))

(defop store :op2 13 nil #{} "store (variable) value"
  [ops store branch text]
  (let [var (second (ops 0))
		value (get-operand-value (ops 1))]
	(set-variable var value)))

(defop insert_obj :op2 14 nil #{} "insert_obj object destination"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		parent (get-operand-value (ops 1))]
	(object-insert obj parent)))

(defop loadw :op2 15 nil #{:store} "loadw array word-index -> (result)"
  [ops store branch text]
  (let [address (get-operand-value (ops 0))
		word-index (get-operand-value (ops 1))]
	(set-variable store (zword (+ address (* word-index 2))))))

(defop loadb :op2 16 nil #{:store} "loadb array byte-index -> (result)"
  [ops store branch text]
  (let [address (get-operand-value (ops 0))
		byte-index (get-operand-value (ops 1))]
	(set-variable store (zbyte (+ address byte-index)))))

(defop get_prop :op2 17 nil #{:store} "get_prop object property -> (result)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		prop (get-operand-value (ops 1))]
	(set-variable store (object-property obj (dec prop)))))

(defop get_prop_addr :op2 18 nil #{:store} "get_prop_addr object property -> (result)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		prop (get-operand-value (ops 1))]
	(set-variable store (object-property-addr obj (dec prop)))))

(defop get_next_prop :op2 19 nil #{:store} 
	   "get_next_prop object property -> (result)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		prop (get-operand-value (ops 1))]
	(set-variable store (object-next-prop obj (dec prop)))))

(defop add :op2 20 nil #{:store} "add a b -> (result)"
  [ops store branch text]
  (let [a (signed-word (get-operand-value (ops 0)))
		b (signed-word (get-operand-value (ops 1)))]
	(set-variable store (+ a b))))

(defop sub :op2 21 nil #{:store} "sub a b -> (result)"
  [ops store branch text]
  (let [a (signed-word (get-operand-value (ops 0)))
		b (signed-word (get-operand-value (ops 1)))]
	(set-variable store (- a b))))

(defop mul :op2 22 nil #{:store} "mul a b -> (result)"
  [ops store branch text]
  (let [a (signed-word (get-operand-value (ops 0)))
		b (signed-word (get-operand-value (ops 1)))]
	(set-variable store (* a b))))

(defop div :op2 23 nil #{:store} "div a b -> (result)"
  [ops store branch text]
  (let [a (signed-word (get-operand-value (ops 0)))
		b (signed-word (get-operand-value (ops 1)))]
	(set-variable store (/ a b))))

(defop mod :op2 24 nil #{:store} "mod a b -> (result)")

(defop call_2s :op2 25 [4] #{:store} "call_2s routine arg1 -> (result)"
  [ops store branch text]
  (op-call (subvec ops 0 2) store))

(defop call_2n :op2 26 [5] #{} "call_2n routine arg1"
  [ops store branch text]
  (op-call (subvec ops 0 2) nil))

(defop set_colour :op2 27 [5] #{} "set_colour foreground background")

(defop set_colour :op2 27 [6] #{} "set_colour foreground background")

(defop throw :op2 28 [5 6 7 8] #{} "throw value stack-frame")

(defop jz :op1 0 nil #{:branch} "jz a ?(label)"
  [ops store branch text]
  (jump-if (zero? (get-operand-value (ops 0))) branch))

(defop get_sibling :op1 1 nil #{:branch :store} "get_sibling object -> (result) ?(label)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		sibling (object-parent obj)]
	(set-variable store sibling)
	(jump-if (not (zero? sibling)) branch)))

(defop get_child :op1 2 nil #{:branch :store} "get_child object -> (result) ?(label)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		parent (object-parent obj)]
	(set-variable store parent)
	(jump-if (not (zero? parent)) branch)))

(defop get_parent :op1 3 nil #{:store} "get_parent object -> (result)"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))]
	(set-variable store (object-parent obj))))

(defop get_prop_len :op1 4 nil #{:store} "get_prop_len property-address -> (result)"
  [ops store branch text]
  (let [prop-addr (get-operand-value (ops 0))]
	(set-variable store (object-property-len prop-addr))))

(defop inc :op1 5 nil #{} "inc (variable)"
  [ops store branch text]
  (let [var (second (ops 0))]
	(inc-variable var)))

(defop dec :op1 6 nil #{} "dec (variable)"
  [ops store branch text]
  (let [var (second (ops 0))]
	(dec-variable var)))

(defop print_addr :op1 7 nil #{} "print_addr byte-address-of-string")

(defop call_1s :op1 8 [4 5 6 7 8] #{:store} "call_1s routine -> (result)"
  [ops store branch text]
  (op-call (subvec ops 0 1) store))

(defop remove_obj :op1 9 nil #{} "remove_obj object"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))]
	(object-remove obj)))

(defop print_obj :op1 10 nil #{} "print_obj object"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))]
	(zprint (object-description obj))))

(defop ret :op1 11 nil #{} "ret value"
  [ops store branch text]
  (routine-return (get-operand-value (ops 0))))

(defop jump :op1 12 nil #{} "jump ?(label)"
  [ops store branch text]
  (let [offset (signed-word (get-operand-value (ops 0)))]
	(set-pc (+ (pc) offset -2))))

(defop print_paddr :op1 13 nil #{} "print_paddr packed-address-of-string"
  [ops store branch text]
  (zprint (read-string (string-address (get-operand-value (ops 0))))))

(defop load :op1 14 nil #{:store} "load (variable) -> (result)")

(defop not :op1 15 [1 2 3 4] #{:store} "not value -> (result)")

(defop call_1n :op1 15 [5 6 7 8] #{} "call_1n routine"
  [ops store branch text]
  (op-call (subvec ops 0 1) nil))

(defop rtrue :op0 0 nil #{} "rtrue"
  [& rest]
  (return-true))

(defop rfalse :op0 1 nil #{} "rfalse"
  [& rest]
  (return-false))

(defop print :op0 2 nil #{:text} "print (literal-string)"
  [ops store branch text]
  (zprint text))

(defop print_ret :op0 3 nil #{:text} "print_ret (literal-string)"
  [ops store branch text]
  (zprintln text)
  (return-true))

(defop nop :op0 4 nil #{} "nop")

(defop save :op0 5 [1 2 3] #{:branch} "save ?(label)")

(defop save :op0 5 [4] #{} "save -> (result)")

(defop restore :op0 6 [1 2 3] #{:branch} "restore ?(label)")

(defop restore :op0 6 [4] #{} "restore -> (result)")

(defop restart :op0 7 nil #{} "restart")

(defop ret_popped :op0 8 nil #{} "ret_popped"
  [ops store branch text]
  (routine-return (stack-pop)))

(defop pop :op0 9 [1] #{} "pop")

(defop catch :op0 9 [5 6 7 8] #{:store} "catch -> (result)")

(defop quit :op0 10 nil #{} "quit")

(defop new_line :op0 11 nil #{} "new_line"
  [ops store branch text]
  (zprintln))

(defop show_status :op0 12 [3] #{} "show_status")

(defop verify :op0 13 [3 4 5 6 7 8] #{:branch} "verify ?(label)")

(defop extended :op0 14 [5 6 7 8] #{} "[first byte of extended opcode]")

(defop piracy :op0 15 [5 6 7 8] #{:branch} "piracy ?(label)")

(defop call :var 0 [1 2 3] #{:store} "call routine ...0 to 3 args... -> (result)
"
  [ops store branch text]
  (op-call ops store))

(defop call_vs :var 0 [4 5 6 7 8] #{:store} "call_vs routine ...0 to 3 args... -> (result)"
  [ops store branch text]
  (op-call ops store))

(defop storew :var 1 nil #{} "storew array word-index value"
  [ops store branch text]
  (let [address (get-operand-value (ops 0))
		word-index (get-operand-value (ops 1))
		value (get-operand-value (ops 2))]
	(set-zword (+ address (* word-index 2)) value)))

(defop storeb :var 2 nil #{} "storeb array byte-index value"
  [ops store branch text]
  (let [address (get-operand-value (ops 0))
		byte-index (get-operand-value (ops 1))
		value (get-operand-value (ops 2))]
	(set-zbyte (+ address byte-index) value)))

(defop put_prop :var 3 nil #{} "put_prop object property value"
  [ops store branch text]
  (let [obj (get-operand-value (ops 0))
		prop (get-operand-value (ops 1))
		value (get-operand-value (ops 2))]
	(object-put-property obj (dec prop) value)))

(defop sread :var 4 [1 2 3] #{} "sread text parse")

(defop sread :var 4 [4] #{} "sread text parse time routine")

(defop aread :var 4 [5 6 7 8] #{:store} "aread text parse time routine -> (result)")

(defop print_char :var 5 nil #{} "print_char output-character-code"
  [ops store branch text]
  (zprint (char (get-operand-value (ops 0)))))

(defop print_num :var 6 nil #{} "print_num value"
  [ops store branch text]
  (zprint (get-operand-value (ops 0))))

(defop random :var 7 nil #{:store} "random range -> (result)")

(defop push :var 8 nil #{} "push value"
  [ops store branch text]
  (let [value (get-operand-value (ops 0))]
	(stack-push value)))

(defop pull :var 9 [1 2 3 4 5] #{} "pull (variable)"
  [ops store branch text]
  (let [var (second (ops 0))]
	(set-variable var (stack-pop))))

(defop pull :var 9 [6 7 8] #{:store} "pull stack -> (result)")

(defop split_window :var 10 [3 4 5 6 7 8] #{} "split_window lines")

(defop set_window :var 11 [3 4 5 6 7 8] #{} "set_window window")

(defop call_vs2 :var 12 [4 5 6 7 8] #{:store} "call_vs2 routine ...0 to 7 args... -> (result)"
  [ops store branch text]
  (op-call ops store))

(defop erase_window :var 13 [4 5 6 7 8] #{} "erase_window window")

(defop erase_line :var 14 [4 5] #{} "erase_line value")

(defop erase_line :var 14 [6 7 8] #{} "erase_line pixels")

(defop set_cursor :var 15 [4 5] #{} "set_cursor line column")

(defop set_cursor :var 15 [6 7 8] #{} "set_cursor line column window")

(defop get_cursor :var 16 [4 5 6 7 8] #{} "get_cursor array")

(defop set_text_style :var 17 [4 5 6 7 8] #{} "set_text_style style")

(defop buffer_mode :var 18 [4 5 6 7 8] #{} "buffer_mode flag")

(defop output_stream :var 19 [3 4] #{} "output_stream number")

(defop output_stream :var 19 [5] #{} "output_stream number table")

(defop output_stream :var 19 [6 7 8] #{} "output_stream number table width")

(defop input_stream :var 20 [3 4 5 6 7 8] #{} "input_stream number")

(defop sound_effect :var 21 [5 6 7 8] #{} "sound_effect number effect volume routine")

(defop read_char :var 22 [4 5 6 7 8] #{:store} "read_char 1 time routine -> (result)")

(defop scan_table :var 23 [4 5 6 7 8] #{:branch :store} "scan_table x table len form -> (result)")

(defop not :var 24 [5 6 7 8] #{:store} "not value -> (result)")

(defop call_vn :var 25 [5 6 7 8] #{} "call_vn routine ...up to 3 args..."
  [ops store branch text]
  (op-call ops nil))

(defop call_vn2 :var 26 [5 6 7 8] #{} "call_vn2 routine ...up to 7 args..."
  [ops store branch text]
  (op-call ops nil))

(defop tokenise :var 27 [5 6 7 8] #{} "tokenise text parse dictionary flag")

(defop encode_text :var 28 [5 6 7 8] #{} "encode_text zscii-text length from coded-text")

(defop copy_table :var 29 [5 6 7 8] #{} "copy_table first second size")

(defop print_table :var 30 [5 6 7 8] #{} "print_table zscii-text width height skip")

(defop check_arg_count :var 31 [5 6 7 8] #{:branch} "check_arg_count argument-number")

(defop save :ext 0 [5 6 7 8] #{:store} "save table bytes name -> (result)")

(defop restore :ext 1 [5 6 7 8] #{:store} "restore table bytes name -> (result)")

(defop log_shift :ext 2 [5 6 7 8] #{:store} "log_shift number places -> (result)")

(defop art_shift :ext 3 [5 6 7 8] #{:store} "art_shift number places -> (result)")

(defop set_font :ext 4 [5 6 7 8] #{:store} "set_font font -> (result)")

(defop draw_picture :ext 5 [6 7 8] #{} "draw_picture picture-number y x")

(defop picture_data :ext 6 [6 7 8] #{:branch} "picture_data picture-number array ?(label)")

(defop erase_picture :ext 7 [6 7 8] #{} "erase_picture picture-number y x")

(defop set_margins :ext 8 [6 7 8] #{} "set_margins left right window")

(defop save_undo :ext 9 [5 6 7 8] #{:store} "save_undo -> (result)")

(defop restore_undo :ext 10 [5 6 7 8] #{:store} "restore_undo -> (result)")

(defop print_unicode :ext 11 [5 6 7 8] #{} "print_unicode char-number")

(defop check_unicode :ext 12 [5 6 7 8] #{} "check_unicode char-number -> (result)")

(defop move_window :ext 16 [6 7 8] #{} "move_window window y x")

(defop window_size :ext 17 [6 7 8] #{} "window_size window y x")

(defop window_style :ext 18 [6 7 8] #{} "window_style window flags operation")

(defop get_wind_prop :ext 19 [6 7 8] #{:store} "get_wind_prop window property-number -> (result)")

(defop scroll_window :ext 20 [6 7 8] #{} "scroll_window window pixels")

(defop pop_stack :ext 21 [6 7 8] #{} "pop_stack items stack")

(defop read_mouse :ext 22 [6 7 8] #{} "read_mouse array")

(defop mouse_window :ext 23 [6 7 8] #{} "mouse_window window")

(defop push_stack :ext 24 [6 7 8] #{:branch} "push_stack value stack ?(label)")

(defop put_wind_prop :ext 25 [6 7 8] #{} "put_wind_prop window property-number value")

(defop print_form :ext 26 [6 7 8] #{} "print_form formatted-table")

(defop make_menu :ext 27 [6 7 8] #{:branch} "make_menu number table ?(label)")

(defop picture_table :ext 28 [6 7 8] #{} "picture_table table")
