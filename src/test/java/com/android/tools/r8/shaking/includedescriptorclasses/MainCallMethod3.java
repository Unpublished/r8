// Copyright (c) 2017, the R8 project authors. Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.android.tools.r8.shaking.includedescriptorclasses;

public class MainCallMethod3 {
  public static void main(String[] args) {
    ClassWithNativeMethods cwnm = new ClassWithNativeMethods();
    // Don't mention the argument or return type.
    cwnm.method1(null);
    cwnm.method2();
    cwnm.method3(null);
  }
}
