package com.timfan.lox;

class Return extends RuntimeException {
  // the return value.
  final Object value; 
  Return(Object value) {
    super(null, null, false, false);
    this.value = value;
  } 
}
