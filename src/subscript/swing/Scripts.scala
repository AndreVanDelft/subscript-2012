package subscript.swing
import scala.swing._
import scala.swing.event._
import subscript.vm._;
import subscript.vm.DSL._
import subscript.Predef._

abstract class SimpleSubscriptApplication extends SimpleSwingApplication{
  override def startup(args: Array[String]) {
    super.startup(args)
    new Thread{override def run={live;quit}}.start()
  }
  def _live(caller: N_call): Unit
  def live: ScriptExecuter
}
object Scripts {
  
  def swing[N<:CallGraphNodeTrait[_]](implicit n:N) = {n.adaptExecuter(new SwingCodeExecuterAdapter[CodeExecuter])}             

  // an extension on scala.swing.Reactor that supports event handling scripts in Subscript
  abstract class ScriptReactor[N<:N_atomic_action_eh[N]] extends Reactor {
    def publisher:Publisher
    var executer: EventHandlingCodeFragmentExecuter[N] = _
    def execute = executeMatching(true)
    def executeMatching(isMatching: Boolean): Unit = executer.executeMatching(isMatching)
    val publisher1 = publisher // needed in subclass since publisher does not seem to be accessible
    private var myEnabled = false
    def enabled = myEnabled
    def enabled_=(b:Boolean) = {myEnabled=b}
    
    val event: Event
    def reaction: PartialFunction[Event,Unit] = myReaction
    private val myReaction: PartialFunction[Event,Unit] = {case event => execute}
    
    def subscribe(n: N): Unit = {
      executer = new EventHandlingCodeFragmentExecuter(n, n.scriptExecuter)
      n.codeExecuter = executer
      val wasAlreadyEnabled = enabled
      publisher.reactions += reaction;
      if (!wasAlreadyEnabled) {enabled=true}
    }
    def unsubscribe: Unit = {
      publisher.reactions -= reaction
      if (!publisher.reactions.isDefinedAt(event)) {enabled=false}
    }
  }
  
  // a ScriptReactor that has a Component as a Publisher. Automatically enables and disables the component
  abstract class ComponentScriptReactor[N<:N_atomic_action_eh[N]](publisher:Publisher with Component) extends ScriptReactor[N] {
    override def enabled_=(b:Boolean) = {
      super.enabled_=(b); 
      publisher.enabled = b
    }
  }
  
  // a ComponentScriptReactor for clicked events on a button
  case class ClickedScriptReactor[N<:N_atomic_action_eh[N]](b:AbstractButton) extends ComponentScriptReactor[N](b) {
    def publisher = b
    val event: Event = ButtonClicked(b)
  }
  
  // a ScriptReactor for key press events
  case class KeyPressScriptReactor[N<:N_atomic_action_eh[N]](publisher:Publisher, keyCode: FormalConstrainedParameter[Char]) extends ScriptReactor[N] {
    // this does not compile: val event: Event = KeyPressed(comp, _, _, _, _)
    val event = null
    override def reaction = myReaction
    private val myReaction: PartialFunction[Event,Unit] = {
      case KeyPressed(comp, keyPressedValue, keyModifiers, keyLocationValue) => 
        if (keyPressedValue.id < 256) {
          val c = keyPressedValue.id.asInstanceOf[Char]
	      if (keyCode.matches(c)) {
	        keyCode.value = c
	        executeMatching(true)
	      }
        }
    }
    override def unsubscribe: Unit = {
      publisher1.reactions -= reaction
      //if (!publisher1.reactions.isDefinedAt(KeyPressed(comp, _, _, _))) {enabled=false}
    }
  }
  
  // a ScriptReactor for virtual key press events
  case class VKeyPressScriptReactor[N<:N_atomic_action_eh[N]](publisher:Publisher, keyValue: FormalConstrainedParameter[Key.Value]) extends ScriptReactor[N] {
    // this does not compile: val event: Event = KeyPressed(comp, _, _, _, _)
    val event = null
    override def reaction = myReaction
    private val myReaction: PartialFunction[Event,Unit] = {
      case KeyPressed(comp, keyPressedValue, keyModifiers, keyLocationValue) => 
        if (keyValue.matches(keyPressedValue)) {
          keyValue.value = keyPressedValue
          executeMatching(true)
        }
    }
    override def unsubscribe: Unit = {
      publisher1.reactions -= reaction
      //if (!publisher1.reactions.isDefinedAt(KeyPressed(comp, _, _, _))) {enabled=false}
    }
  }
  
/* the following subscript code has manually been compiled into Scala; see below
 
 scripts
  clicked(b:Button) = 
    val csr = ClickedScriptReactor(b)
    @swing:  // the redirection to the swing thread is needed because enabling and disabling the button must there be done
    @csr.subscribe(there); there.onDeactivate{()=>csr.unsubscribe}: {. .}
    
  key(comp: Component, keyCode: Char??) =
    val ksr = KeyPressScriptReactor(keyCode)
    @csr.subscribe(there); there.onDeactivate{()=>csr.unsubscribe}: {. .}
 
  vkey(comp: Component, keyValue: Key.Value??) =
    val ksr = KeyPressScriptReactor(keyValue)
    @csr.subscribe(there); there.onDeactivate{()=>csr.unsubscribe}: {. .}
 
 Note: the manual compilation yielded for the first annotation the type
  
   N_annotation[N_annotation[N_code_eh]]
   
 All the complicated generic type parameters on TemplateNodes and CallGraphNodes were needed
 to make it easy enforceable that "there" and even "there.there" would be of the proper type
*/
  
  def _clicked(caller: N_call, _b:FormalInputParameter[Button])  = {
   _script(caller, 'clicked, param(_b,'b),
        T_n_ary(";", 
         T_0_ary_code  ("val" , (_here:                           N_localvar ) => {implicit val here =_here; here.initLocalVariable('csr, new ClickedScriptReactor[N_code_eh](_b.value))}),
         T_1_ary_code  ("@:"  , ( here: N_annotation[N_annotation[N_code_eh]]) => {implicit val there=here.there; swing}, 
          T_1_ary_code ("@:"  , ( here:              N_annotation[N_code_eh] ) => {implicit val there=here.there;
                                                                                                here.withLocal('csr, 0, (_csr: LocalVariable[ClickedScriptReactor[N_code_eh]]) =>
                                                                                                                        {_csr.value.subscribe(there); there.onDeactivate{()=>_csr.value.unsubscribe}})}, 
           T_0_ary_code("{..}", (_here:                           N_code_eh  ) => {}//{implicit val here =_here; println("\nCLICKED!!!")} // Temporary tracing
    ))))) 
  }
               
  def _key(caller: N_call, _publisher: FormalInputParameter[Publisher], _keyCode: FormalConstrainedParameter[Char])  = {
   _script(caller, 'key, param(_publisher,'publisher), param(_keyCode,'keyCode),
        T_n_ary(";", 
         T_0_ary_code ("val" , (_here:                           N_localvar ) => {implicit val  here=_here; here.initLocalVariable('ksr, new KeyPressScriptReactor[N_code_eh](_publisher.value, _keyCode))}),
         T_1_ary_code ("@:"  , ( here:              N_annotation[N_code_eh] ) => {implicit val there=here.there;
                                                                                                here.withLocal('ksr, 0, (_ksr: LocalVariable[ClickedScriptReactor[N_code_eh]]) =>
                                                                                                                        {_ksr.value.subscribe(there); there.onDeactivate{()=>_ksr.value.unsubscribe}})},
          T_0_ary_code("{..}", (_here:                           N_code_eh  ) => {}//{implicit val here=_here; println("\nKey"+_keyCode.value)} // Temporary tracing
    ))))
  }
               
 def _vkey(caller: N_call, _publisher: FormalInputParameter[Publisher], _keyValue: FormalConstrainedParameter[Key.Value])  = {
  _script(caller, 'key, param(_publisher,'publisher), param(_keyValue,'keyValue),
    T_n_ary(";", 
     T_0_ary_code ("val" , (_here:                           N_localvar ) => {implicit val  here=_here; here.initLocalVariable('ksr, new VKeyPressScriptReactor[N_code_eh](_publisher.value, _keyValue))}),
     T_1_ary_code ("@:"  , ( here:              N_annotation[N_code_eh] ) => {implicit val there=here.there;
                                                                                            here.withLocal('ksr, 0, (_ksr: LocalVariable[ClickedScriptReactor[N_code_eh]]) =>
                                                                                                                    {_ksr.value.subscribe(there); there.onDeactivate{()=>_ksr.value.unsubscribe}})},
      T_0_ary_code("{..}", (_here:                           N_code_eh  ) => {}//{implicit val here=_here; println("\nVKey"+_keyValue.value)} // Temporary tracing
    ))))
  }
               
  // bridge methods
  def clicked(   b:Button                        ): ScriptExecuter = {val executer=new BasicExecuter; _clicked(executer.anchorNode, b); executer.run}
  def     key(comp: Component, keyCode: Char     ): ScriptExecuter = {val executer=new BasicExecuter;     _key(executer.anchorNode, comp, keyCode); executer.run}
  def    vkey(comp: Component, keyCode: Key.Value): ScriptExecuter = {val executer=new BasicExecuter;    _vkey(executer.anchorNode, comp, keyCode); executer.run}

}