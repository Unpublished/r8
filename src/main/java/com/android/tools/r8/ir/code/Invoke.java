// Copyright (c) 2016, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package com.android.tools.r8.ir.code;

import com.android.tools.r8.code.MoveResult;
import com.android.tools.r8.code.MoveResultObject;
import com.android.tools.r8.code.MoveResultWide;
import com.android.tools.r8.dex.Constants;
import com.android.tools.r8.errors.Unreachable;
import com.android.tools.r8.graph.AppInfoWithSubtyping;
import com.android.tools.r8.graph.DexEncodedMethod;
import com.android.tools.r8.graph.DexItem;
import com.android.tools.r8.graph.DexMethod;
import com.android.tools.r8.graph.DexProto;
import com.android.tools.r8.graph.DexType;
import com.android.tools.r8.ir.conversion.DexBuilder;
import java.util.List;

public abstract class Invoke extends Instruction {

  public enum Type {
    DIRECT,
    INTERFACE,
    STATIC,
    SUPER,
    VIRTUAL,
    NEW_ARRAY,
    CUSTOM,
    POLYMORPHIC
  }

  public Invoke(Value result, List<Value> arguments) {
    super(result, arguments);
  }

  public static Invoke create(
      Type type, DexItem target, DexProto proto, Value result, List<Value> arguments) {
    switch (type) {
      case DIRECT:
        return new InvokeDirect((DexMethod) target, result, arguments);
      case INTERFACE:
        return new InvokeInterface((DexMethod) target, result, arguments);
      case STATIC:
        return new InvokeStatic((DexMethod) target, result, arguments);
      case SUPER:
        return new InvokeSuper((DexMethod) target, result, arguments);
      case VIRTUAL:
        return new InvokeVirtual((DexMethod) target, result, arguments);
      case NEW_ARRAY:
        return new InvokeNewArray((DexType) target, result, arguments);
      case CUSTOM:
        throw new Unreachable("Use InvokeCustom constructor instead");
      case POLYMORPHIC:
        return new InvokePolymorphic((DexMethod) target, proto, result, arguments);
    }
    throw new Unreachable("Unknown invoke type: " + type);
  }

  public static Instruction createFromTemplate(
      Invoke template, Value outValue, List<Value> inValues) {
    if (template.isInvokeMethod()) {
      return create(template.getType(),
          template.asInvokeMethod().getInvokedMethod(),
          template.isInvokePolymorphic() ? template.asInvokePolymorphic().getProto() : null,
          outValue,
          inValues);
    }

    if (template.isInvokeNewArray()) {
      return new InvokeNewArray(template.asInvokeNewArray().getArrayType(), outValue, inValues);
    }

    assert template.isInvokeCustom();
    InvokeCustom custom = template.asInvokeCustom();
    return new InvokeCustom(custom.getCallSite(), outValue, inValues);
  }

  abstract public Type getType();

  public List<Value> arguments() {
    return inValues;
  }

  public int requiredArgumentRegisters() {
    int registers = 0;
    for (Value inValue : inValues) {
      registers += inValue.requiredRegisters();
    }
    return registers;
  }

  protected int argumentRegisterValue(int i, DexBuilder builder) {
    if (i < arguments().size()) {
      return builder.allocatedRegisterForRangedArgument(arguments().get(i), getNumber());
    }
    return 0;
  }

  protected int fillArgumentRegisters(DexBuilder builder, int[] registers) {
    int i = 0;
    for (Value value : arguments()) {
      int register = builder.allocatedRegister(value, getNumber());
      for (int j = 0; j < value.requiredRegisters(); j++) {
        assert i < 5;
        registers[i++] = register++;
      }
    }
    return i;
  }

  protected boolean hasHighArgumentRegister(DexBuilder builder) {
    for (Value value : arguments()) {
      if (builder.argumentValueUsesHighRegister(value, getNumber())) {
        return true;
      }
    }
    return false;
  }

  protected boolean argumentsConsecutive(DexBuilder builder) {
    Value value = arguments().get(0);
    int next = builder.allocatedRegisterForRangedArgument(value, getNumber()) + value.requiredRegisters();
    for (int i = 1; i < arguments().size(); i++) {
      value = arguments().get(i);
      assert next == builder.allocatedRegisterForRangedArgument(value, getNumber());
      next += value.requiredRegisters();
    }
    return true;
  }

  protected void addInvokeAndMoveResult(com.android.tools.r8.code.Instruction instruction, DexBuilder builder) {
    if (outValue != null && outValue.needsRegister()) {
      int register = builder.allocatedRegister(outValue, getNumber());
      com.android.tools.r8.code.Instruction moveResult;
      switch (outType()) {
        case SINGLE:
          moveResult = new MoveResult(register);
          break;
        case WIDE:
          moveResult = new MoveResultWide(register);
          break;
        case OBJECT:
          moveResult = new MoveResultObject(register);
          break;
        default:
          throw new Unreachable("Unexpected result type " + outType());
      }
      builder.add(this, new com.android.tools.r8.code.Instruction[]{instruction, moveResult});
    } else {
      builder.add(this, instruction);
    }
  }

  @Override
  public boolean instructionTypeCanThrow() {
    return true;
  }

  @Override
  public int maxInValueRegister() {
    if (requiredArgumentRegisters() > 5) {
      return Constants.U16BIT_MAX;
    }
    return Constants.U4BIT_MAX;
  }

  @Override
  public int maxOutValueRegister() {
    return Constants.U8BIT_MAX;
  }

  abstract protected String getTypeString();

  @Override
  public String getInstructionName() {
    return "Invoke-" + getTypeString();
  }

  // This method is used for inlining.
  // It returns the target method iff this invoke has only one target.
  abstract public DexEncodedMethod computeSingleTarget(AppInfoWithSubtyping appInfo);

  @Override
  public boolean isInvoke() {
    return true;
  }

  @Override
  public Invoke asInvoke() {
    return this;
  }
}