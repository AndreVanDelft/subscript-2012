package subscript.test

import subscript._
import subscript.DSL._
import subscript.Predef._
import subscript.vm._

// Subscript sample application: "Hello world!", printed using a sequence of calls to a default script that prints its string parameter
//
// Note: the main part of this source file has been manually compiled from Subscript code into plain Scala

object TestHelloWorld_withCall {
 
// script..
//  main(args: Array[String]) = "hello" "world!"
//  implicit printlnScript(s: String) = println(s)
//
// would be translated to 2*2 = 4 methods
  
// script methods, to be called from bridge-to-Scala method or from other scripts
// Note: the DSL cannot take advantage of the "implicit" _printlnScript, since the parameters "hello" and "world!" 
// would first need to be boxed to a ActualValueParameter, before _printlnScript would apply 
         def _main (_args: FormalInputParameter[Array[String]]) = _script(this, 'main, _args~'args)    {_seq(_call{_printlnScript("hello")}, _printlnScript("world!"))}
implicit def _printlnScript(_s:  FormalInputParameter[String] ) = {_script(this, 'printlnScript, _s~'s) { {println(_s.value)} }}

// bridge methods. Most return a ScriptExecuter; 
// only a "main" method with the proper parameter type has return type Unit, to serve as a program entry point 
// implicit scripts have no bridge methods; implicit functions would be more appropriate
def main(args: Array[String]): Unit = _execute(_main(args))
}