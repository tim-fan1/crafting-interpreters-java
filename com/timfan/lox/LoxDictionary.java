package com.timfan.lox;
import java.util.Map;
public class LoxDictionary {
  public Map<Object,Object> dictionary;
  LoxDictionary(Map<Object, Object> dictionary) {
    this.dictionary = dictionary;
  }
  @Override
  public String toString() {
    return "<dictionary>";
  }
}
