/* Generated By:JJTree: Do not edit this line. ASTGTNode.java */

package org.faktorips.fl.parser;

public class ASTGTNode extends SimpleNode {
  public ASTGTNode(int id) {
    super(id);
  }

  public ASTGTNode(FlParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(FlParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
