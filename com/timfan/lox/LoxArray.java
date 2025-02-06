package com.timfan.lox;
import java.util.List;
public class LoxArray {
  public List<Object> list;
  LoxArray(List<Object> list) {
    this.list = list;
  }
  @Override
  public String toString() {
    return "<array>";
  }
}
