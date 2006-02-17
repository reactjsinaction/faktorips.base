package org.faktorips.fl.operations;

import org.faktorips.datatype.Datatype;
import org.faktorips.fl.CompilerAbstractTest;
import org.faktorips.fl.BinaryOperation;
import org.faktorips.fl.ExprCompiler;



/**
 *
 */
public class GreaterDecimalDecimalTest extends CompilerAbstractTest {
    
    protected void setUp() throws Exception {
        super.setUp();
        compiler.setBinaryOperations(new BinaryOperation[]{new GreaterThanDecimalDecimal()});
    }
    
    public void testSuccessfull() throws Exception {
        execAndTestSuccessfull("3.5 > 3.4", Boolean.TRUE, Datatype.BOOLEAN);
    }

    public void testLhsError() throws Exception {
        execAndTestFail("a a > 3.5", ExprCompiler.SYNTAX_ERROR);
    }
    
    public void testRhsError() throws Exception {
        execAndTestFail("3 > a a", ExprCompiler.SYNTAX_ERROR);
    }
    
}
