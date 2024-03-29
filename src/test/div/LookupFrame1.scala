package subscript.test
import scala.swing._
import scala.swing.event._
import subscript.Predef._
import subscript.swing._
import subscript.swing.Scripts._
import subscript._
import subscript.DSL._
import subscript.Predef._
import subscript.vm._

// Subscript sample application: a text entry field with a search button, that simulates the invocation of a background search
//
// Note: the main part of this source file has been manually compiled from Subscript code into plain Scala
object LookupFrame1 extends LookupFrame1Application

class LookupFrame1Application extends LookupFrameApplication {
  
  top.listenTo (searchTF.keys)
  val f = top.peer.getRootPane().getParent().asInstanceOf[javax.swing.JFrame]
  f.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE)

  /* the following subscript code has manually been compiled into Scala; see below
  script..
   override searchCommand        = searchButton + Key.Enter 
   implicit vkey(k: Key.Value??) = vkey(top, k??)
*/

  override def _searchCommand                         = _script(this, 'searchCommand) {_alt(_clicked(searchButton), _vkey(Key.Enter))}
  def _vkey(_k:FormalConstrainedParameter[Key.Value]) = _script(this, 'vkey, _k~??'k) {subscript.swing.Scripts._vkey(top, _k~??)}
}
