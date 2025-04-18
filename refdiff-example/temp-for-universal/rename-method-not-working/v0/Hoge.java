package tmp;

public class Hoge {
  public void hello() { // 記述量が少なく、Renameとして検出されない
    System.out.println("Hello!");
  }

  public void m1(String arg) {
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

  public void lorem() {
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
