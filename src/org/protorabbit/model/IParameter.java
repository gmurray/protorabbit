package org.protorabbit.model;

public interface IParameter {

  public static final int NULL = 0;
  public static final int STRING = 1;
  public static final int NUMBER = 2;
  public static  final int BOOLEAN = 3;
  public static final int ARRAY = 4;
  public static final int OBJECT = 5;

  public Object getValue();
  public int getType();
}
