// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
package inlining;

import inlining.pkg.OtherPublicClass;
import inlining.pkg.PublicClass;
import inlining.pkg.Subclass;

class A {

  int a;

  A(int a) {
    this.a = a;
  }

  int a() {
    return a;
  }

  int cannotInline(int v) {
    // Cannot inline due to recursion.
    if (v > 0) {
      return cannotInline(v - 1);
    }
    return 42;
  }
}

class B extends A {

  B(int a) {
    super(a);
  }

  int cannotInline(int v) {
    return -1;
  }

  int callMethodInSuper() {
    return super.cannotInline(10);
  }
}

class InlineConstructor {

  int a;

  @CheckDiscarded
  InlineConstructor(int a) {
    this.a = a;
  }

  InlineConstructor(long a) {
    this((int) a);
  }

  InlineConstructor(int a, int loopy) {
    this.a = a;
    // Make this too big to inline.
    if (loopy > 10) {
      throw new RuntimeException("Too big!");
    }
    for (int i = 1; i < loopy; i++) {
      this.a = this.a * i;
    }
  }

  @CheckDiscarded
  InlineConstructor() {
    this(42, 9);
  }

  static InlineConstructor create() {
    return new InlineConstructor(10L);
  }

  static InlineConstructor createMore() {
    new InlineConstructor(0, 0);
    new InlineConstructor(0, 0);
    new InlineConstructor(0, 0);
    new InlineConstructor(0, 0);
    new InlineConstructor(0, 0);
    new InlineConstructor(0, 0);
    return new InlineConstructor();
  }
}

class InlineConstructorOfInner {

  class Inner {

    int a;

    @CheckDiscarded
    Inner(int a) {
      this.a = a;
    }

    // This is not inlined, even though it is only called once, as it is only called from a
    // non-constructor, and will set a field (the outer object) before calling the other
    // constructor.
    Inner(long a) {
      this((int) a);
    }

    public Inner create() {
      return new Inner(10L);
    }
  }

  Inner inner;

  InlineConstructorOfInner() {
    inner = new Inner(10L).create();
  }
}

public class Inlining {

  private static void Assert(boolean value) {
    if (!value) {
      System.out.println("FAILURE");
    }
  }

  public static void main(String[] args) {
    // Ensure the simple methods are called at least three times, to not be inlined due to being
    // called only once or twice.
    Assert(intExpression());
    Assert(intExpression());
    Assert(intExpression());
    Assert(longExpression());
    Assert(longExpression());
    Assert(longExpression());
    Assert(doubleExpression());
    Assert(floatExpression());
    Assert(floatExpression());
    Assert(floatExpression());
    Assert(stringExpression());
    Assert(stringExpression());
    Assert(stringExpression());

    Assert(intArgumentExpression());
    Assert(intArgumentExpression());
    Assert(intArgumentExpression());
    Assert(longArgumentExpression());
    Assert(longArgumentExpression());
    Assert(longArgumentExpression());
    Assert(doubleArgumentExpression());
    Assert(doubleArgumentExpression());
    Assert(doubleArgumentExpression());
    Assert(floatArgumentExpression());
    Assert(floatArgumentExpression());
    Assert(floatArgumentExpression());
    Assert(stringArgumentExpression());
    Assert(stringArgumentExpression());
    Assert(stringArgumentExpression());

    Assert(intAddExpression());
    Assert(intAddExpression());
    Assert(intAddExpression());

    A b = new B(42);
    A a = new A(42);
    Assert(intCmpExpression(a, b));
    Assert(intCmpExpression(a, b));
    Assert(intCmpExpression(a, b));

    // This is only called once!
    Assert(onlyCalledOnce(10));

    // This is only called twice, and is quite small!
    Assert(onlyCalledTwice(1) == 2);
    Assert(onlyCalledTwice(1) == 2);

    InlineConstructor ic = InlineConstructor.create();
    Assert(ic != null);
    InlineConstructor ic2 = InlineConstructor.createMore();
    Assert(ic2 != null);
    InlineConstructorOfInner icoi = new InlineConstructorOfInner();
    Assert(icoi != null);

    // Check that super calls are processed correctly.
    new B(123).callMethodInSuper();

    // Inline calls to package private methods
    PublicClass.alsoCallsPackagePrivateMethod();
    OtherPublicClass.callsMethodThatCallsPackagePrivateMethod();
    // Inline calls to protected methods.
    PublicClass.callsProtectedMethod3();
    PublicClass.alsoReadsPackagePrivateField();
    OtherPublicClass.callsMethodThatCallsProtectedMethod();
    OtherPublicClass.callsMethodThatReadsFieldInPackagePrivateClass();
    Subclass.callsMethodThatCallsProtectedMethod();
    // Do not inline constructors which set final field.
    System.out.println(new InlineConstructorFinalField());

    // Call method three times to ensure it would not normally be inlined but force inline anyway.
    int aNumber = longMethodThatWeShouldNotInline("ha", "li", "lo");
    aNumber += longMethodThatWeShouldNotInline("zi", "za", "zo");
    aNumber += longMethodThatWeShouldNotInline("do", "de", "da");
    System.out.println(aNumber);
  }

  private static boolean intCmpExpression(A a, A b) {
    return a.a() == b.a();
  }

  @CheckDiscarded
  private static int intConstantInline() {
    return 42;
  }

  @CheckDiscarded
  private static boolean intExpression() {
    return 42 == intConstantInline();
  }

  @CheckDiscarded
  private static long longConstantInline() {
    return 50000000000L;
  }

  @CheckDiscarded
  private static boolean longExpression() {
    return 50000000000L == longConstantInline();
  }

  @CheckDiscarded
  private static double doubleConstantInline() {
    return 42.42;
  }

  @CheckDiscarded
  private static boolean doubleExpression() {
    return 42.42 == doubleConstantInline();
  }

  @CheckDiscarded
  private static float floatConstantInline() {
    return 21.21F;
  }

  @CheckDiscarded
  private static boolean floatExpression() {
    return 21.21F == floatConstantInline();
  }

  @CheckDiscarded
  private static String stringConstantInline() {
    return "Fisk er godt";
  }

  private static boolean stringExpression() {
    return "Fisk er godt" == stringConstantInline();
  }

  @CheckDiscarded
  private static int intArgumentInline(int a, int b, int c) {
    return b;
  }

  @CheckDiscarded
  private static boolean intArgumentExpression() {
    return 42 == intArgumentInline(-2, 42, -1);
  }

  @CheckDiscarded
  private static long longArgumentInline(long a, long b, long c) {
    return c;
  }

  @CheckDiscarded
  private static boolean longArgumentExpression() {
    return 50000000000L == longArgumentInline(-2L, -1L, 50000000000L);
  }

  @CheckDiscarded
  private static double doubleArgumentInline(double a, double b, double c) {
    return a;
  }

  @CheckDiscarded
  private static boolean doubleArgumentExpression() {
    return 42.42 == doubleArgumentInline(42.42, -2.0, -1.0);
  }

  @CheckDiscarded
  private static float floatArgumentInline(float a, float b, float c) {
    return b;
  }

  @CheckDiscarded
  private static boolean floatArgumentExpression() {
    return 21.21F == floatArgumentInline(-2.0F, 21.21F, -1.0F);
  }

  @CheckDiscarded
  private static String stringArgumentInline(String a, String b, String c) {
    return c;
  }

  private static boolean stringArgumentExpression() {
    return "Fisk er godt" == stringArgumentInline("-1", "-1", "Fisk er godt");
  }

  @CheckDiscarded
  private static int intAddInline(int a, int b) {
    return a + b;
  }

  @CheckDiscarded
  private static boolean intAddExpression() {
    return 42 == intAddInline(21, 21);
  }

  @CheckDiscarded
  private static boolean onlyCalledOnce(int count) {
    int anotherCounter = 0;
    for (int i = 0; i < count; i++) {
      anotherCounter += i;
    }
    return anotherCounter > count;
  }

  @CheckDiscarded
  private static int onlyCalledTwice(int count) {
    return count > 0 ? count + 1 : count - 1;
  }

  @AlwaysInline
  @CheckDiscarded
  private static int longMethodThatWeShouldNotInline(String a, String b, String c) {
    String result = a + b + c + b + a + c + b;
    return result.length();
  }
}
