import scala.swing._
import scala.swing.event._
import subscript.Predef._
import subscript.swing.SimpleSubscriptApplication
import subscript.swing.Scripts._
import subscript._
import subscript.DSL._
import subscript.Predef._
import subscript.vm._

// Subscript sample application: A..B
//
// Note: the main part of this source file has been manually compiled from Subscript code into plain Scala

object AB extends ABApplication

class ABApplication extends SimpleSubscriptApplication {
  
  val A = new Button("A")           {enabled       = false}
  val B = new Button("B")           {enabled       = false}
  //val X = new Button("Exit")        {enabled       = false}
  val ABLabel  = new Label("..A;B") {preferredSize = new Dimension(45,26)}
  val outputTA = new TextArea       {editable      = false}
  
  val top          = new MainFrame {
    title          = "..A;B; exit - Subscript"
    location       = new Point    (0,0)
    preferredSize  = new Dimension(300,70)
    contents       = new BorderPanel {
      add(new FlowPanel(A, B, ABLabel), BorderPanel.Position.North) 
    }
  }
  
/* the following subscript code has manually been compiled into Scala; see below
 script..
  override live = A..B; exit
           exit = X+Key.Excape
*/

  override def _live = _script(this, 'live) {_seq(_seq(_optionalBreak_loop, _clicked(A)), _clicked(B))}
        //def _exit = _script('exit) {_alt(_clicked(X), _vkey(top, Key.Escape))}
               
  // bridge methods; only the first one is actually used; implicit scripts do not get bridge methods   
  override def live = _execute(_live)
        //def exit = _execute(_exit)
}
