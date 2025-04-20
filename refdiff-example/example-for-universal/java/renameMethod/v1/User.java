package pkg;

public class User {
  private int age;

  public User(int age) {
    this.age = age;
  }
  
  public boolean isAdult() {
    return age >= 18;
  }
}
