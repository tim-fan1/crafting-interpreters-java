package com.timfan.lox;

class Break extends RuntimeException {
  Break() {
    super(null, null, false, false);
  } 
}
