package subscript.vm

import java.awt.{Font, BasicStroke, Stroke, Color => AWTColor}
import java.awt.geom.AffineTransform
import scala.collection.mutable.ListBuffer
import scala.swing._
import subscript.swing._
import subscript.swing.Scripts._
import subscript.DSL._

object GraphicalDebugger extends GraphicalDebuggerApp {
  override def main(args: Array[String]): Unit = {
    var lArgs = args
    if (lArgs.isEmpty) return
    ScriptExecutorFactory.scriptDebugger = this
    top.visible = true
    
    lArgs.head match {
      case "-s" => lArgs = lArgs.tail; if (lArgs.isEmpty) return; descriptionLabel.text = lArgs.head
                   lArgs = lArgs.tail; if (lArgs.isEmpty) return
      case _ =>
    }
    new Thread{override def run={
      live;
      quit
    }}.start()
    
    try {
      val c = Class.forName(lArgs.head) // TBD: should be a swing application
      val m = c.getMethod("main", classOf[Array[String]])
      m.invoke(null, lArgs.tail)
    }
    catch {
      case e: ClassNotFoundException =>
    }
  }
}
//    

class GraphicalDebuggerApp extends SimpleSubscriptApplication with ScriptDebugger {

  
  // TBD:
  // create live method
  // create GUI
  // test
  // draw call graph
  // draw message lists

  var messageBeingHandled = false
  var currentMessage: CallGraphMessage[_] = null
  
  def interestingContinuationInternals(c: Continuation): List[String] = {
     var ss: List[String] = Nil
     if (c != null) {
          if ( c.success       != null) ss ::= "Success"
          if (!c.aaEndeds     .isEmpty) ss ::= "AA Ended"
          if (!c.aaStarteds   .isEmpty) ss ::= "AA Started"
          if (!c.deactivations.isEmpty) ss ::= "Deactivations"
          if ( c.activation    != null) ss ::= "Activation"
     }
     ss
  }
  
  val exitButton = new Button("Exit"  ) {enabled = false; defaultCapable = false; focusable=false }
  val stepButton = new Button("Step"  ) {enabled = false}
  val callGraphPanel = new Panel {
    background = AWTColor.white
    preferredSize  = new Dimension(1000,2000)
    override def paint(g: Graphics2D) {
        g.setColor(AWTColor.white)
        g.fillRect(0, 0, size.width, size.height)
        onPaintCallGraph(g)
    }
  }
  val templateTreesPanel = new Panel {
    background = AWTColor.white
    preferredSize  = new Dimension(500,500)
    override def paint(g: Graphics2D) {
        g.setColor(AWTColor.white)
        g.fillRect(0, 0, size.width, size.height)
        onPaintTemplateTrees(g)
    }
  }
  val fixedWidthFont = new Font("Monaco", Font.BOLD, 12)
  val     normalFont = new Font("Arial" , Font.BOLD, 16)
  val   normalStroke = new BasicStroke(1)
  val      fatStroke = new BasicStroke(3)
  
  val GRID_W  = 120
  val GRID_H  =  43
  val RATIO_W = 0.8
  val RATIO_H = 0.6
  val BOX_W   = (GRID_W * RATIO_W).toInt
  val BOX_H   = (GRID_H * RATIO_H).toInt
  
  val hOffset = (GRID_W - BOX_W)/2
  val vOffset = (GRID_H - BOX_H)/2

  
  def drawStringCentered(g: Graphics2D, s: String, cx: Int, cy: Int) {
    val normalFontMetrics = g.getFontMetrics(normalFont)
    val sw = normalFontMetrics.stringWidth(s)
    val sh = normalFontMetrics.getHeight
    g.drawString(s, cx-sw/2, cy+sh/2)
  }
  def drawStringTopLeft(g: Graphics2D, s: String, x: Int, y: Int) {
    val normalFontMetrics = g.getFontMetrics(normalFont)
    val sh = normalFontMetrics.getHeight
    g.drawString(s, x, y+sh/2)
  }
  def emphasize_g(g: Graphics2D, doIt: Boolean) {
    if (doIt) {
      g.setColor(AWTColor.red)
      g.setStroke(fatStroke)
    }
    else {
      g.setColor(AWTColor.black)
      g.setStroke(normalStroke)
    }
  }
  def onPaintTemplateTrees(g: Graphics2D) {
    def emphasize(doIt: Boolean) {emphasize_g(g, doIt)}

    def getScriptTemplates: List[T_script] = {
      val lb = new ListBuffer[T_script]
      def getScriptTemplates(n: CallGraphNodeTrait[_]): Unit = {
        n match {case ns: N_script                    => if (!lb.contains(ns.template)) lb += ns.template case _ =>}
        n match {case pn: CallGraphParentNodeTrait[_] =>pn.forEachChild{getScriptTemplates(_)} case _ =>}
      } 
      getScriptTemplates(rootNode)
      lb.toList
    }
    def drawEdge(p: TemplateNode, pwx: Double, pwy: Int, 
                 c: TemplateNode, cwx: Double, cwy: Int): Unit = {
        
        val pHCenter = (pwx*GRID_W).toInt + BOX_W/2 + hOffset
        val pBottom  =  pwy*GRID_H        + BOX_H   + vOffset
        val cHCenter = (cwx*GRID_W).toInt + BOX_W/2 + hOffset
        val cTop     =  cwy*GRID_H                  + vOffset
        
        val x1       = pHCenter
        val y1       = pBottom
        val x2       = cHCenter
        val y2       = cTop
        
        g.drawLine(x1, y1, x2, y2)
      }
    def drawTemplateTree(t: TemplateNode, xGrid: Double, yGrid: Int): (Double, Double) = {
        var resultW = 0d
        var childHCs = new ListBuffer[Double]
	    if (t.children.isEmpty) resultW = 1d
	    else {
             t.children.foreach{ ct =>
	            val (childW, childHC) = drawTemplateTree(ct, xGrid+resultW, yGrid+1)
	            resultW  += childW
	            childHCs += childHC
              }
	    }
        val thisX   = xGrid+(resultW-1)/2 
        val boxLeft = (thisX*GRID_W).toInt+hOffset
        val boxTop  = yGrid*GRID_H+vOffset
        val hCenter = boxLeft + BOX_W/2
        val vCenter = boxTop  + BOX_H/2
        
        val r = new Rectangle(boxLeft, boxTop, BOX_W, BOX_H)
        g.setColor(new AWTColor(255, 242, 230)) 
        g fill r
        val n = currentMessage.node.asInstanceOf[CallGraphNode[_<:TemplateNode]]
        val isCurrentTemplate = currentMessage != null && n.template == t
        emphasize(isCurrentTemplate)
        g draw r
        emphasize(false)
        drawStringCentered(g, t.toString(), hCenter, vCenter)
        (t.children zip childHCs).foreach{ c_hc: (TemplateNode, Double) =>
          drawEdge(t, thisX, yGrid, c_hc._1, c_hc._2, yGrid+1)
        }
        (resultW, thisX)
    }
    
    if (scriptExecutor==null) return
    g.setFont(normalFont)
    var currentXGrid = 0d // drawn width of template trees so far, in Grid coordinates
    getScriptTemplates.foreach { t => currentXGrid += drawTemplateTree(t, currentXGrid, 0)._1}
  }
  def onPaintCallGraph(g: Graphics2D) {
      def emphasize(doIt: Boolean) {emphasize_g(g, doIt)}
      
      g.setFont(normalFont)

      def drawArrow(x1: Int, y1: Int, x2: Int, y2: Int, s: String) {
        val dx    = x2 - x1
        val dy    = y2 - y1
        val angle = math.atan2(dy, dx)
        val len   = math.sqrt(dx*dx + dy*dy).intValue
        val ARROW_HEAD_W = 5
        val ARROW_HEAD_L = 15
        
        emphasize(s != null)
        if (s != null) {
          drawStringTopLeft(g, s, x1 + dx/2 + 9, y1 + dy/2 - 2)
        }
        val oldTransform = g.getTransform()
        val at = oldTransform.clone.asInstanceOf[AffineTransform]
        at.concatenate(AffineTransform.getTranslateInstance(x1, y1))
        at.concatenate(AffineTransform.getRotateInstance(angle))
        g.setTransform(at)

        // Draw horizontal arrow starting in (0, 0)
        g.drawLine(0, 0, len, 0)
        
        if (s != null) {
          g.fillPolygon(Array(len, len-ARROW_HEAD_L, len-ARROW_HEAD_L, len),
                        Array(  0,    -ARROW_HEAD_W,     ARROW_HEAD_W,   0), 4)
        }
        g.setTransform(oldTransform)
        emphasize(false)
      }
      def getBreakText(a: ActivationMode.ActivationModeType): String = {
        a match {
          case ActivationMode.Optional => "Optional Break"
          case _ => "Break"
        }
      }
      def drawEdge(p: CallGraphNodeTrait[_], pwx: Double, pwy: Int, 
                   c: CallGraphNodeTrait[_], cwx: Double, cwy: Int): Unit = {
        
        val pHCenter = (pwx*GRID_W).toInt + BOX_W/2 + hOffset
        val pBottom  =  pwy*GRID_H        + BOX_H   + vOffset
        val cHCenter = (cwx*GRID_W).toInt + BOX_W/2 + hOffset
        val cTop     =  cwy*GRID_H                  + vOffset
        
        val x1       = pHCenter
        val y1       = pBottom
        val x2       = cHCenter
        val y2       = cTop
        
        if (currentMessage!=null) {
          currentMessage match {
            case AAStarted(p1,c1) if (p==p1 && c==c1)  => drawArrow(x2, y2, x1, y1, "AA Started")
            case AAEnded  (p1,c1) if (p==p1 && c==c1)  => drawArrow(x2, y2, x1, y1, "AA Ended")
            case Success  (p1,c1) if (p==p1 && c==c1)  => drawArrow(x2, y2, x1, y1, "Success")
            case Break    (p1,c1, activationMode) 
                                  if (p==p1 && c==c1)  => drawArrow(x2, y2, x1, y1,  getBreakText(activationMode))
            case Exclude  (p1,c1) if (p==p1 && c==c1)  => drawArrow(x1, y1, x2, y2, "Exclude")
            case _                                     => drawArrow(x1, y1, x2, y2, null)
          }
        }
        else drawArrow(x1, y1, x2, y2, null)
        
        g.setStroke(normalStroke)
        g.setColor (AWTColor.black)
      }
      def drawContinuationTexts(n: CallGraphNodeTrait[_], boxRight: Int, boxTop: Int) = {
        val x = boxRight + 3
        var y = boxTop
        n match {
          case nn: N_n_ary_op =>     
            val normalFontMetrics = g.getFontMetrics(normalFont)
            interestingContinuationInternals(nn.continuation).foreach{s: String=>drawStringTopLeft(g, s, x, y); y += normalFontMetrics.getHeight+2}
          case _ => if (currentMessage!=null&&currentMessage.node==n) currentMessage match {
            case s: Success   if (s.child==null) => drawStringTopLeft(g, "Success"  , x, y) 
            case a: AAStarted if (a.child==null) => drawStringTopLeft(g, "AAStarted", x, y) 
            case a: AAEnded   if (a.child==null) => drawStringTopLeft(g, "AAEnded"  , x, y) 
            case _ => 
          }
        }
      }
	  def drawTree[T <: TemplateNode](n: CallGraphNodeTrait[T], xGrid: Double, yGrid: Int): (Double, Double) = {
        var resultW = 0d // drawn width of this subtree
        var childHCs = new ListBuffer[Double]
        
        val isCurrentNode = currentMessage != null && currentMessage.node == n
	    n match {
	      case p:CallGraphParentNodeTrait[_] => 
	        val pcl=p.children.length
	        if (pcl==0) {
	          resultW = 1
	        }
	        else {
              p.children.foreach{ c =>
	            val (childW, childHC) = drawTree(c, xGrid+resultW, yGrid+1)
	            resultW  += childW
	            childHCs += childHC
              }
	        }
	      case _ => resultW = 1
	    }
        val thisX   = xGrid+(resultW-1)/2 
        val boxLeft = (thisX*GRID_W).toInt+hOffset
        val boxTop  = yGrid*GRID_H+vOffset
        val hCenter = boxLeft + BOX_W/2
        val vCenter = boxTop  + BOX_H/2
        
        val s: String = n match {
          case ns: N_script   => ns.template.name.name
          case no: N_n_ary_op => no.template.kind + (if (no.isIteration) " ..." else "")
          case _              => n .template.kind
        }
        
        val r = new Rectangle(boxLeft, boxTop, BOX_W, BOX_H)
        g.setColor(new AWTColor(230, 255, 230)) 
        g fill r
        emphasize(isCurrentNode)
        g draw r 
        if (isCurrentNode) {
          currentMessage match {
            case d: Deactivation =>
              n match {
                case pn: CallGraphParentNodeTrait[_] if (pn.children.length>0) =>
                case _ =>
                  g.drawLine(boxLeft, boxTop      , boxLeft+BOX_W, boxTop+BOX_H)
                  g.drawLine(boxLeft, boxTop+BOX_H, boxLeft+BOX_W, boxTop      )
              }
            case _ =>
          }
        }
        drawContinuationTexts(n, boxLeft+BOX_W, boxTop)
        emphasize(false)
        drawStringTopLeft (g, n.index.toString, boxLeft+2, boxTop+5)
        drawStringCentered(g, s, hCenter, vCenter)
        
	    n match {
	      case p:CallGraphParentNodeTrait[_] => 
	        (p.children zip childHCs).foreach{ c_hc: (CallGraphNodeTrait[_], Double) =>
	          drawEdge(n, thisX, yGrid, c_hc._1, c_hc._2, yGrid+1)
	        }
	      case _ =>
	    }
        (resultW, thisX)
	  }
      if (scriptExecutor!=null) drawTree(rootNode, 0, 0)
  }
  
  val msgLogListModel   = new javax.swing.DefaultListModel
  val msgQueueListModel = new javax.swing.DefaultListModel
  
  val currentMessageTF  = new TextField {
    preferredSize       = new Dimension(400,24)
    minimumSize         = preferredSize
    editable            = false
    font                = normalFont
    horizontalAlignment = scala.swing.Alignment.Left
  }
  val msgLogList        = new ListBuffer[String]
  val msgQueueList      = new ListBuffer[String]
  val msgLogListView    = new ListView(msgLogList) {
    font                = fixedWidthFont
    peer.setModel(msgLogListModel)
  }
  val msgQueueListView  = new ListView(msgQueueList) {
    font                = fixedWidthFont
    peer.setModel(msgQueueListModel)
  }
  val msgLogListViewScrollPane   = new ScrollPane
  {
  	contents                     = msgLogListView
  	verticalScrollBarPolicy      = ScrollPane.BarPolicy.Always
  }
  val msgQueueListViewScrollPane = new ScrollPane
  {
  	contents                     = msgQueueListView
  	verticalScrollBarPolicy      = ScrollPane.BarPolicy.Always
  }
  val splitPaneGraphs = new SplitPane(scala.swing.Orientation.Horizontal, new ScrollPane(templateTreesPanel), 
                                                                          new ScrollPane(callGraphPanel)                       ) {dividerLocation  = 150}
  val splitPaneMsgs   = new SplitPane(scala.swing.Orientation.Horizontal, msgLogListViewScrollPane,  msgQueueListViewScrollPane) {dividerLocation  = 350}
  val splitPaneMain   = new SplitPane(scala.swing.Orientation.Vertical,   splitPaneMsgs,             splitPaneGraphs           ) {dividerLocation  = 240}

  val descriptionLabel = new Label {
    preferredSize      = new Dimension(300,24)
    font               = normalFont
  }
  val top              = new Frame {
    title              = "Subscript Graphical Debugger"
    location           = new Point    (0,00)
    preferredSize      = new Dimension(800,600)
    contents           = new BorderPanel {
      add(new FlowPanel(currentMessageTF, stepButton, exitButton, descriptionLabel), BorderPanel.Position.North) 
      add(splitPaneMain, BorderPanel.Position.Center)
    }
  }
  
  def sleep(duration_ms: Long) = try {Thread.sleep(duration_ms)} catch {case e: InterruptedException => println("sleep interrupted")}
  def confirmExit: Boolean = Dialog.showConfirmation(null, "Are you sure?", "About to exit")==Dialog.Result.Yes
  
  
  def awaitMessageBeingHandled(value: Boolean) = {
    var sleeptime = 1
    while (messageBeingHandled!=value) {
      sleep(sleeptime)
      if (sleeptime<100) sleeptime *=2
    }
  }
  def shouldStep: Boolean =
    currentMessage match {
      case Activation(_) | Deactivation(_,_,_) | AAStarted(_,_) | AAEnded(_,_) | Success(_,_)  | Break(_,_,_)  | Exclude(_,_) => true
      case c:Continuation if (!interestingContinuationInternals(c).isEmpty) => true
      case _ => false
    }
  
  def logMessage_GUIThread(m: String, msg: CallGraphMessage[_]) {
      var runnable = new Runnable {
        def run(): Unit = {logMessage(m, msg)}
      }
      javax.swing.SwingUtilities.invokeLater(runnable)
  }
  def logMessage(m: String, msg: CallGraphMessage[_]) {
    msgLogListModel.addElement(m + " " + msg)
    msgLogListViewScrollPane.verticalScrollBar.value = msgLogListViewScrollPane.verticalScrollBar.maximum
    msgQueueListModel.clear
    scriptExecutor.scriptGraphMessages.foreach(msgQueueListModel.addElement(_)) 
  }
  def updateDisplay = {
    var s = currentMessage.toString
    if (s.length>50) s = s.substring(0, 50) + "..."
    currentMessageTF.text = s
    logMessage (">>", currentMessage)
    callGraphPanel    .repaint()
    templateTreesPanel.repaint()
  }
  // script..
  //   live       = {*awaitMessageBeingHandled*}
  //                if (shouldStep) ( @gui: {!updateDisplay!}; stepCommand || if(!needsStep(m)) waitSomeTime )
  //                {messageBeingHandled=false}
  //                ...
  //             || exitDebugger
  //
  // stepCommand  = stepButton
  // exitCommand  = exitButton
  // exitDebugger = exitCommand @gui: while(!confirmExit)
  
  override def _live  = _script('live) {_par_or2(_seq(_threaded{awaitMessageBeingHandled(true)}, 
                                                      _if{shouldStep} (_seq(_at{gui} (_tiny{updateDisplay}), _stepCommand)), 
                                                      _normal{messageBeingHandled=false}, 
                                                      _loop
                                                     ), 
                                                  _exitDebugger
                                                )}
  def   _stepCommand  = _script('stepCommand ) {_clicked(stepButton)}
  def   _exitCommand  = _script('exitCommand ) {_clicked(exitButton)} // windowClosing
  def   _exitDebugger = _script('exitDebugger) {_seq(  _exitCommand, _at{gui} (_while{!confirmExit}))}
  
  override def live = _execute(_live, false) //), new SimpleScriptDebugger)
  
  def scriptGraphMessages = scriptExecutor.scriptGraphMessages
  def rootNode            = scriptExecutor.rootNode
  
  def messageHandled(m: CallGraphMessage[_]): Unit = {
    currentMessage = m
    messageBeingHandled=true 
    awaitMessageBeingHandled(false)
    currentMessage = null
  }
  def messageQueued      (m: CallGraphMessage[_]                 ) = logMessage_GUIThread("++", m)
  def messageDequeued    (m: CallGraphMessage[_]                 ) = logMessage_GUIThread("--", m)
  def messageContinuation(m: CallGraphMessage[_], c: Continuation) = logMessage_GUIThread("**", c)
  def messageAwaiting: Unit = {
    currentMessageTF.text = "Waiting..."
    callGraphPanel.repaint()
}
}