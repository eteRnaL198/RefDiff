package tmp;

public class Hoge {
  public void greet() { // 記述量が少なく、Renameとして検出されない
    System.out.println("Hello!");
  }

  // public void greet2() {
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  //   System.out.println("Hello!");
  // }
  
  public void m1Renamed(String arg) {
    m2();
    m3();
    m4();
    m5();
    m6();
    m7();
    m8();
    m9();
    m10();
  }

  public void loremRenamed() {
    ipsum();
    dolor();
    sit();
    amet();
    consectetur();
    adipiscing();
    elit();
    sed();
    eiusmod();
    tempor();
    incididunt();
    labore();
    dolore();
    magna();
    aliqua();
    ut();
    enim();
    ad();
    minim();
    veniam();
  }
}
