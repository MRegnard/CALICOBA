package fr.irit.smac.util;

import msi.gama.common.interfaces.IValue;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.getter;
import msi.gama.precompiler.GamlAnnotations.variable;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.types.IType;

@vars({
    @variable(name = Triplet.FIRST, type = IType.NONE),
    @variable(name = Triplet.SECOND, type = IType.NONE),
    @variable(name = Triplet.THIRD, type = IType.NONE),
})
@doc("A triplet is a container that holds 3 values.")
public class Triplet<T1, T2, T3> implements IValue {
  public static final String FIRST = "first";
  public static final String SECOND = "second";
  public static final String THIRD = "third";

  private final T1 value1;
  private final T2 value2;
  private final T3 value3;

  public Triplet(T1 value1, T2 value2, T3 value3) {
    this.value1 = value1;
    this.value2 = value2;
    this.value3 = value3;
  }

  @getter(FIRST)
  public T1 getFirst() {
    return this.value1;
  }

  @getter(SECOND)
  public T2 getSecond() {
    return this.value2;
  }

  @getter(THIRD)
  public T3 getThird() {
    return this.value3;
  }

  @Override
  public String toString() {
    return String.format("(%s, %s, %s)", this.value1, this.value2, this.value3);
  }

  @Override
  public String stringValue(IScope scope) throws GamaRuntimeException {
    return this.toString();
  }

  @Override
  public IValue copy(IScope scope) throws GamaRuntimeException {
    return new Triplet<>(this.getFirst(), this.getSecond(), this.getThird());
  }
}
