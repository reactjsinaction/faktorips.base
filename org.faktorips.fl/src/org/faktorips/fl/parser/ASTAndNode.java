/* Generated By:JJTree: Do not edit this line. ASTAndNode.java */

package org.faktorips.fl.parser;

public class ASTAndNode extends SimpleNode {
  public ASTAndNode(int id) {
    super(id);
  }

  public ASTAndNode(FlParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(FlParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
