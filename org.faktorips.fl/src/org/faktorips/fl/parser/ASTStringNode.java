/* Generated By:JJTree: Do not edit this line. ASTStringNode.java */

package org.faktorips.fl.parser;

public class ASTStringNode extends SimpleNode {
  public ASTStringNode(int id) {
    super(id);
  }

  public ASTStringNode(FlParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(FlParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
