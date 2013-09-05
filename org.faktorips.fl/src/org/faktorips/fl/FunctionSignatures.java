/*******************************************************************************
 * Copyright (c) 2005-2012 Faktor Zehn AG und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorzehn.org/f10-org:lizenzen:community eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn AG - initial API and implementation - http://www.faktorzehn.de
 *******************************************************************************/

package org.faktorips.fl;

import static org.faktorips.datatype.Datatype.BOOLEAN;
import static org.faktorips.datatype.Datatype.DECIMAL;
import static org.faktorips.datatype.Datatype.DOUBLE;
import static org.faktorips.datatype.Datatype.INTEGER;
import static org.faktorips.datatype.Datatype.MONEY;
import static org.faktorips.datatype.Datatype.PRIMITIVE_BOOLEAN;
import static org.faktorips.datatype.Datatype.PRIMITIVE_INT;
import static org.faktorips.datatype.Datatype.PRIMITIVE_LONG;

import org.faktorips.datatype.AnyDatatype;
import org.faktorips.datatype.ArrayOfValueDatatype;
import org.faktorips.datatype.Datatype;
import org.faktorips.fl.functions.SumBeanArrayPropertyFct;

/**
 * A list of all function signatures that are supported by the formula language.
 */
public enum FunctionSignatures {
    /**
     * Returns the absolute value of the argument.<br/>
     * {@code Decimal=Abs(Decimal)}
     * 
     * @see Datatype#DECIMAL
     */
    Abs(DECIMAL, new Datatype[] { DECIMAL }),
    /**
     * Returns whether all arguments are {@code true}.<br/>
     * {@code boolean=And(boolean...)}
     * 
     * @see Datatype#PRIMITIVE_BOOLEAN
     */
    And(PRIMITIVE_BOOLEAN, PRIMITIVE_BOOLEAN),
    /**
     * Returns whether the argument is not {@code null} or a Null-Object.<br/>
     * {@code boolean=Exists(Object)}
     * 
     * @see Datatype#PRIMITIVE_BOOLEAN
     * @see AnyDatatype
     */
    Exists(PRIMITIVE_BOOLEAN, new Datatype[] { AnyDatatype.INSTANCE }),
    /**
     * Returns the second argument if the first argument is {@code true}, the third argument if not.<br/>
     * {@code Object=If(boolean, Object, Object)}
     * 
     * @see Datatype#PRIMITIVE_BOOLEAN
     * @see AnyDatatype
     */
    If(AnyDatatype.INSTANCE, new Datatype[] { PRIMITIVE_BOOLEAN, AnyDatatype.INSTANCE, AnyDatatype.INSTANCE }),
    /**
     * Returns whether the argument is an empty array, {@code null} or a Null-Object.<br/>
     * {@code boolean=IsEmpty(Object)}
     * 
     * @see Datatype#PRIMITIVE_BOOLEAN
     * @see AnyDatatype
     */
    IsEmpty(PRIMITIVE_BOOLEAN, new Datatype[] { AnyDatatype.INSTANCE }),
    /**
     * Returns the maximum of the two arguments.<br/>
     * {@code Decimal=Max(Decimal,Decimal)}
     * 
     * @see Datatype#DECIMAL
     */
    MaxDecimal(DECIMAL, new Datatype[] { DECIMAL, DECIMAL }),
    /**
     * Returns the maximum of the two arguments.<br/>
     * {@code double=Max(double,double)}
     * 
     * @see Datatype#DOUBLE
     */
    MaxDouble(DOUBLE, new Datatype[] { DOUBLE, DOUBLE }),
    /**
     * Returns the maximum of the two arguments.<br/>
     * {@code int=Max(int,int)}
     * 
     * @see Datatype#PRIMITIVE_INT
     */
    MaxInt(PRIMITIVE_INT, new Datatype[] { PRIMITIVE_INT, PRIMITIVE_INT }),
    /**
     * Returns the maximum of the two arguments.<br/>
     * {@code long=Max(long,long)}
     * 
     * @see Datatype#PRIMITIVE_LONG
     */
    MaxLong(PRIMITIVE_LONG, new Datatype[] { PRIMITIVE_LONG, PRIMITIVE_LONG }),
    /**
     * Returns the maximum of the two arguments.<br/>
     * {@code Money=Max(Money,Money)}
     * 
     * @see Datatype#MONEY
     */
    MaxMoney(MONEY, new Datatype[] { MONEY, MONEY }),
    /**
     * Returns the minimum of the two arguments.<br/>
     * {@code Decimal=Min(Decimal,Decimal)}
     * 
     * @see Datatype#DECIMAL
     */
    MinDecimal(DECIMAL, new Datatype[] { DECIMAL, DECIMAL }),
    /**
     * Returns the minimum of the two arguments.<br/>
     * {@code double=Min(double,double)}
     * 
     * @see Datatype#DOUBLE
     */
    MinDouble(DOUBLE, new Datatype[] { DOUBLE, DOUBLE }),
    /**
     * Returns the minimum of the two arguments.<br/>
     * {@code int=Min(int,int)}
     * 
     * @see Datatype#PRIMITIVE_INT
     */
    MinInt(PRIMITIVE_INT, new Datatype[] { PRIMITIVE_INT, PRIMITIVE_INT }),
    /**
     * Returns the minimum of the two arguments.<br/>
     * {@code long=Min(long,long)}
     * 
     * @see Datatype#PRIMITIVE_LONG
     */
    MinLong(PRIMITIVE_LONG, new Datatype[] { PRIMITIVE_LONG, PRIMITIVE_LONG }),
    /**
     * Returns the minimum of the two arguments.<br/>
     * {@code Decimal=Min(Money,Money)}
     * 
     * @see Datatype#MONEY
     */
    MinMoney(MONEY, new Datatype[] { MONEY, MONEY }),
    /**
     * Returns the inverted argument.<br/>
     * {@code boolean=Not(boolean)}
     * 
     * @see Datatype#PRIMITIVE_BOOLEAN
     */
    Not(PRIMITIVE_BOOLEAN, new Datatype[] { PRIMITIVE_BOOLEAN }),
    /**
     * Returns the inverted argument.<br/>
     * {@code Boolean=Not(Boolean)}
     * 
     * @see Datatype#BOOLEAN
     */
    NotBoolean(BOOLEAN, new Datatype[] { BOOLEAN }),
    /**
     * Returns whether one of the arguments is {@code true}.<br/>
     * {@code boolean=Or(boolean...)}
     * 
     * @see Datatype#PRIMITIVE_BOOLEAN
     */
    Or(PRIMITIVE_BOOLEAN, PRIMITIVE_BOOLEAN),
    /**
     * Returns the first argument, rounded to the scale given by the second argument.<br/>
     * {@code Decimal=Round(Decimal, int)}
     * 
     * @see Datatype#DECIMAL
     */
    Round(DECIMAL, new Datatype[] { DECIMAL, PRIMITIVE_INT }),
    /**
     * Returns the first argument, rounded down to the scale given by the second argument.<br/>
     * {@code Decimal=RoundDown(Decimal, int)}
     * 
     * @see Datatype#DECIMAL
     */
    RoundDown(DECIMAL, new Datatype[] { DECIMAL, PRIMITIVE_INT }),
    /**
     * Returns the first argument, rounded up to the scale given by the second argument.<br/>
     * {@code Decimal=RoundUp(Decimal, int)}
     * 
     * @see Datatype#DECIMAL
     */
    RoundUp(DECIMAL, new Datatype[] { DECIMAL, PRIMITIVE_INT }),
    /**
     * Given an array of objects as the first argument returns the sum of the properties identified
     * by the second argument.<br/>
     * {@code Object=Sum(Object[], Property)}
     * 
     * @see SumBeanArrayPropertyFct
     * @see AnyDatatype
     */
    SumBeanArrayPropertyFct(AnyDatatype.INSTANCE, new Datatype[] { AnyDatatype.INSTANCE, AnyDatatype.INSTANCE }),
    /**
     * Returns the sum of the values in the argument array.<br/>
     * {@code Decimal=Sum(Decimal[])}
     * 
     * @see Datatype#DECIMAL
     */
    SumDecimal(DECIMAL, new Datatype[] { new ArrayOfValueDatatype(DECIMAL, 1) }),
    /**
     * Returns the argument, rounded down to an Integer.<br/>
     * {@code Integer=WholeNumber(Decimal)}
     * 
     * @see Datatype#INTEGER
     * @see Datatype#DECIMAL
     */
    WholeNumber(INTEGER, new Datatype[] { DECIMAL }),
    /**
     * Returns the Decimal argument, to the power of the Integer value.<br/>
     * {@code Decimal=ValueOf(Decimal.bigDecimalValue().pow(Integer)}
     */
    PowerDecimal(DECIMAL, new Datatype[] { DECIMAL, INTEGER }),
    /**
     * Still need to be documented
     */
    Root(DECIMAL, new Datatype[] { DECIMAL });

    private final Datatype type;
    private final Datatype[] argTypes;
    private final boolean hasVarArgs;

    private FunctionSignatures(Datatype type, Datatype argType) {
        this.type = type;
        this.hasVarArgs = true;
        this.argTypes = new Datatype[] { argType };
    }

    private FunctionSignatures(Datatype type, Datatype[] argTypes) {
        this.type = type;
        this.hasVarArgs = false;
        this.argTypes = argTypes;
    }

    public Datatype getType() {
        return type;
    }

    public Datatype[] getArgTypes() {
        Datatype[] copy = new Datatype[argTypes.length];
        System.arraycopy(argTypes, 0, copy, 0, argTypes.length);
        return copy;
    }

    public boolean hasVarArgs() {
        return hasVarArgs;
    }
}
