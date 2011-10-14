package subscript

import subscript.vm._

object DSL {
  
  def _param[T<:Any](p:       FormalInputParameter[T], n:Symbol) = {p.bindToFormalInputParameter      ; p.asInstanceOf[FormalParameter_withName[T]].nameThis(n)}
  def _param[T<:Any](p:      FormalOutputParameter[T], n:Symbol) = {p.bindToFormalOutputParameter     ; p.asInstanceOf[FormalParameter_withName[T]].nameThis(n)}
  def _param[T<:Any](p: FormalConstrainedParameter[T], n:Symbol) = {p.bindToFormalConstrainedParameter; p.asInstanceOf[FormalParameter_withName[T]].nameThis(n)}

  def _execute(_script: N_call => Unit) = {val executer = new BasicExecuter; _script(executer.anchorNode); executer.run}
  
  implicit def codeFragment_to_T_0_ary_code(codeFragment: => Unit): T_0_ary_code[N_code_normal] = T_0_ary_code("{}", (_here:N_code_normal) => codeFragment)
  implicit def codeFragment_here_to_T_0_ary_code(codeFragment: (N_code_normal => Unit)): T_0_ary_code[N_code_normal] = T_0_ary_code("{}", codeFragment)
    
           def _threaded(codeFragment: => Unit): T_0_ary_code[N_code_threaded] = T_0_ary_code("{**}", (_here:N_code_threaded) => codeFragment)
  implicit def codeFragment_here_to_T_0_ary_code_threaded(codeFragment: (N_code_threaded => Unit)): T_0_ary_code[N_code_threaded] = T_0_ary_code("{**}", codeFragment)
    
  implicit def scriptCall_to_T_0_ary_code(_scriptCall: N_call=>Unit) =  T_0_ary_code("call", (_here: N_call) => {_scriptCall.apply(_here)})

  implicit def annotation_to_T_1_ary_code[N<:CallGraphNodeTrait[T],T<:TemplateNode](_annotation: N_annotation[N] => Unit, _child: T) =
    T_1_ary_code("@:", _annotation, _child)

  def _op1(opSymbol: String)(child   : TemplateNode ) = T_1_ary(opSymbol, child)
  def _op (opSymbol: String)(children: TemplateNode*) = T_n_ary(opSymbol, children:_*)
  
  def _seq           = _op (";")_
  def _alt           = _op ("+")_
  def _par           = _op ("&")_
  def _par_or        = _op ("|")_
  def _par_and2      = _op ("&&")_
  def _par_or2       = _op ("||")_
  def _disrupt       = _op ("/")_
  def _not           = _op1("!")_
  def _not_react     = _op1("-")_
  def _react         = _op1("~")_
  def _launch        = _op1("*")_
  def _launch_anchor = _op1("**")_

  def _empty                             = T_0_ary("(+)")
  def _deadlock                          = T_0_ary("(-)")
  def _neutral                           = T_0_ary("(+-)")
  def _break                             = T_0_ary("break")
  def _optionalBreak                     = T_0_ary(".")
  def _loop_optionalBreak                = T_0_ary("..")
  def _loop                              = T_0_ary("...")
  def _while(_cond:       =>Boolean)     = T_0_ary_test("while", (here: N_while ) => _cond)
  def _while(_cond:N_while=>Boolean)     = T_0_ary_test("while",                     _cond)
  
  def _script(name: Symbol, p: FormalParameter_withName[_]*) = (_t: TemplateNode) => ((_c: N_call) => _c.calls(T_script("script", name, _t), p:_*))
}