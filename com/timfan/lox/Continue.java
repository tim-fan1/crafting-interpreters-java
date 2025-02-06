package com.timfan.lox;

class Continue extends RuntimeException {
  Continue() {
    super(null, null, false, false);
  } 
}
